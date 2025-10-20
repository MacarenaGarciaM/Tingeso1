package com.example.demo.services;

import com.example.demo.entities.LoanEntity;
import com.example.demo.entities.LoanItemEntity;
import com.example.demo.entities.ToolEntity;
import com.example.demo.entities.UserEntity;
import com.example.demo.repositories.LoanRepository;
import com.example.demo.repositories.ToolRepository;
import com.example.demo.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final ToolRepository toolRepository;
    private final UserRepository userRepository;
    private final ToolService toolService;
    private final UserService userService;

    private static final int DAILY_RENT_PRICE = 2500;

    @Transactional
    public LoanEntity createLoan(
            String rutUser,
            LocalDate reservationDate,
            LocalDate returnDate,
            List<Item> items
    ) {
        if (reservationDate == null || returnDate == null)
            throw new IllegalArgumentException("Reservation and return dates are required.");
        if (returnDate.isBefore(reservationDate))
            throw new IllegalArgumentException("Return date cannot be before reservation date.");
        if (items == null || items.isEmpty())
            throw new IllegalArgumentException("At least one item is required.");

        UserEntity customer = userRepository.findByRut(rutUser);
        if (customer == null) throw new IllegalArgumentException("User (rut) not found: " + rutUser);

        // Bloqueo por vencidos/multas impagas
        userService.recomputeActiveStatus(rutUser);
        UserEntity refreshed = userRepository.findByRut(rutUser);
        if (refreshed != null && !refreshed.isActive()) {
            throw new IllegalArgumentException("User is inactive due to overdue loans or unpaid fines.");
        }

        // Máximo 5 préstamos activos
        long activeCount = loanRepository.countByRutUserAndLateReturnDateIsNull(rutUser);
        if (activeCount >= 5)
            throw new IllegalArgumentException("User already has 5 active loans.");

        // Cabecera
        LoanEntity loan = new LoanEntity();
        loan.setRutUser(customer.getRut());
        loan.setReservationDate(reservationDate);
        loan.setReturnDate(returnDate);
        loan.setLateReturnDate(null);
        loan.setLateFine(0);
        loan.setDamagePenalty(0);

        // Calcular total (mínimo 1 día)
        loan.setTotal(calculateLoanTotal(reservationDate, returnDate));

        // Para kardex (RUT del cliente)
        UserEntity kardexUser = new UserEntity();
        kardexUser.setRut(customer.getRut());

        Set<Long> seen = new HashSet<>();
        for (Item it : items) {
            if (it == null || it.toolId == null)
                throw new IllegalArgumentException("Each item requires 'toolId'.");

            if (!seen.add(it.toolId))
                throw new IllegalArgumentException("Tool repeated in the same loan: " + it.toolId);

            int qty = (it.quantity == null) ? 1 : it.quantity;
            if (qty <= 0) throw new IllegalArgumentException("quantity must be >= 1");
            if (qty != 1) throw new IllegalArgumentException("Only one unit per tool is allowed.");

            // Cargamos SOLO UNA VEZ la herramienta "Disponible"
            ToolEntity disponibleTool = toolRepository.findById(it.toolId)
                    .orElseThrow(() -> new IllegalArgumentException("Tool not found (id=" + it.toolId + ")"));

            if (!"Disponible".equalsIgnoreCase(disponibleTool.getInitialState()))
                throw new IllegalArgumentException("Tool id=" + it.toolId + " is not 'Disponible'.");
            if (disponibleTool.getAmount() < qty)
                throw new IllegalArgumentException("Not enough stock for tool id=" + it.toolId +
                        ". Available: " + disponibleTool.getAmount());

            // === Validación de "misma herramienta ya arrendada por el mismo usuario" ===
            // Busca ids de herramientas en estado PRESTADA con el mismo nombre+categoría
            List<Long> prestadaIds = toolRepository.findIdsByNameCategoryAndState(
                    disponibleTool.getName(), disponibleTool.getCategory(), "Prestada");

            if (!prestadaIds.isEmpty()) {
                boolean alreadyActive = loanRepository.existsActiveWithAnyToolId(rutUser, prestadaIds);
                if (alreadyActive) {
                    throw new IllegalArgumentException(
                            "El usuario ya tiene un préstamo activo de esta herramienta (" +
                                    disponibleTool.getName() + " - " + disponibleTool.getCategory() + ")."
                    );
                }
            }
            // ===========================================================================

            // Mover Disponible -> Prestada y OBTENER el registro/bucket en "Prestada"
            ToolEntity prestadaTool =
                    toolService.updateTool(it.toolId, "Prestada", null, null, kardexUser);

            // Guardar en loan_item el ID de la herramienta "Prestada"
            LoanItemEntity line = new LoanItemEntity();
            line.setTool(prestadaTool);
            line.setToolNameSnapshot(prestadaTool.getName());
            loan.addItem(line);
        }

        LoanEntity saved = loanRepository.save(loan);

        // Contador de préstamos activos +1 y recomputar 'active'
        customer.setAmountOfLoans(customer.getAmountOfLoans() + 1);
        userRepository.save(customer);
        userService.recomputeActiveStatus(rutUser);

        return saved;
    }

    @Transactional
    public LoanEntity returnLoan(
            Long loanId,
            LocalDate actualReturnDate,
            Set<Long> damagedToolIds,      // opcional (IDs de loan_item.tool.id en estado Prestada)
            Set<Long> irreparableToolIds,  // opcional (IDs de loan_item.tool.id en estado Prestada)
            Integer finePerDay
    ) {
        if (actualReturnDate == null) throw new IllegalArgumentException("actualReturnDate is required.");

        LoanEntity loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + loanId));

        if (loan.getLateReturnDate() != null)
            throw new IllegalArgumentException("Loan is already returned (closed).");

        // Normalizar sets
        damagedToolIds = (damagedToolIds == null) ? Collections.emptySet() : damagedToolIds;
        irreparableToolIds = (irreparableToolIds == null) ? Collections.emptySet() : irreparableToolIds;

        // No se pueden superponer
        Set<Long> inter = new HashSet<>(damagedToolIds);
        inter.retainAll(irreparableToolIds);
        if (!inter.isEmpty())
            throw new IllegalArgumentException("A tool cannot be both damaged and irreparable: " + inter);

        // Validar que los IDs correspondan al préstamo (IDs de herramientas en estado Prestada)
        Set<Long> loanToolIds = new HashSet<>();
        for (LoanItemEntity li : loan.getItems()) loanToolIds.add(li.getTool().getId());

        if (!loanToolIds.containsAll(damagedToolIds)) {
            Set<Long> unknown = new HashSet<>(damagedToolIds);
            unknown.removeAll(loanToolIds);
            throw new IllegalArgumentException("Damaged IDs not in this loan: " + unknown);
        }
        if (!loanToolIds.containsAll(irreparableToolIds)) {
            Set<Long> unknown = new HashSet<>(irreparableToolIds);
            unknown.removeAll(loanToolIds);
            throw new IllegalArgumentException("Irreparable IDs not in this loan: " + unknown);
        }

        // Para kardex
        UserEntity kardexUser = new UserEntity();
        kardexUser.setRut(loan.getRutUser());

        int damagePenalty = 0;

        for (LoanItemEntity line : loan.getItems()) {
            Long prestadaId = line.getTool().getId();
            ToolEntity tool = line.getTool();

            if (irreparableToolIds.contains(prestadaId)) {
                Integer replacement = tool.getRepositionValue(); // ajusta el getter si se llama diferente
                if (replacement == null) replacement = 0;
                damagePenalty += replacement;
                toolService.updateTool(prestadaId, "Dada de baja", null, null, kardexUser);

            } else if (damagedToolIds.contains(prestadaId)) {
                toolService.updateTool(prestadaId, "En reparación", null, null, kardexUser);

            } else {
                toolService.updateTool(prestadaId, "Disponible", null, null, kardexUser);
            }
        }

        // Multa por atraso
        int fineRate = (finePerDay == null) ? 0 : Math.max(0, finePerDay);
        long lateDays = Math.max(0, ChronoUnit.DAYS.between(loan.getReturnDate(), actualReturnDate));
        int lateFine = (int) (lateDays * (long) fineRate);

        loan.setLateReturnDate(actualReturnDate);
        loan.setLateFine(lateFine);
        loan.setDamagePenalty(damagePenalty);

        // Flags impagas por defecto si hay montos
        if (lateFine > 0) loan.setLateFinePaid(false);
        if (damagePenalty > 0) loan.setDamagePenaltyPaid(false);

        LoanEntity saved = loanRepository.save(loan);

        // Contador de préstamos activos -1
        UserEntity customer = userRepository.findByRut(loan.getRutUser());
        if (customer != null) {
            customer.setAmountOfLoans(Math.max(0, customer.getAmountOfLoans() - 1));
            userRepository.save(customer);
        }

        // Recalcular 'active' (si ya no hay vencidos ni multas impagas, vuelve a true)
        userService.recomputeActiveStatus(loan.getRutUser());

        return saved;
    }

    @Transactional
    public LoanEntity payFines(Long loanId, boolean payLateFine, boolean payDamagePenalty) {
        LoanEntity loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + loanId));

        if (payLateFine && loan.getLateFine() > 0) {
            loan.setLateFinePaid(true);
        }
        if (payDamagePenalty && loan.getDamagePenalty() > 0) {
            loan.setDamagePenaltyPaid(true);
        }

        LoanEntity saved = loanRepository.save(loan);

        // Recalcular 'active' del dueño del préstamo
        userService.recomputeActiveStatus(loan.getRutUser());
        return saved;
    }

    // ==== Helpers ====

    private int calculateLoanTotal(LocalDate reservationDate, LocalDate returnDate) {
        long days = ChronoUnit.DAYS.between(reservationDate, returnDate);
        if (days < 1) days = 1; // mínimo 1 día
        return (int) (days * DAILY_RENT_PRICE);
    }

    public List<LoanEntity> listActiveLoans(String rutUser) {
        return loanRepository.findByRutUserAndLateReturnDateIsNull(rutUser);
    }

    public List<LoanEntity> listAllActiveLoans() {
        return loanRepository.findByLateReturnDateIsNull();
    }

    public Page<LoanEntity> listLoansWithUnpaidDebts(String rutUser,
                                                     LocalDate start,
                                                     LocalDate end,
                                                     Pageable pageable) {
        String rut = (rutUser != null && rutUser.isBlank()) ? null : rutUser;

        boolean hasStart = (start != null);
        boolean hasEnd   = (end   != null);

        return loanRepository.findLoansWithUnpaidDebts(
                rut,
                hasStart, start,
                hasEnd,   end,
                pageable
        );
    }


    // Body para creación (sin DTOs externos)
    public static class Item {
        public Long toolId;
        public Integer quantity;
        public Item() {}
    }
}