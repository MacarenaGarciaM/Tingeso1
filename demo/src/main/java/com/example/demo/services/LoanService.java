// src/main/java/com/example/demo/services/LoanService.java
package com.example.demo.services;

import com.example.demo.entities.LoanEntity;
import com.example.demo.entities.LoanItemEntity;
import com.example.demo.entities.ToolEntity;
import com.example.demo.entities.UserEntity;
import com.example.demo.repositories.LoanRepository;
import com.example.demo.repositories.ToolRepository;
import com.example.demo.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
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

        // only 5 loans
        long activeCount = loanRepository.countByRutUserAndLateReturnDateIsNull(rutUser);
        if (activeCount >= 5)
            throw new IllegalArgumentException("User already has 5 active loans.");


        LoanEntity loan = new LoanEntity();
        loan.setRutUser(customer.getRut());
        loan.setReservationDate(reservationDate);
        loan.setReturnDate(returnDate);
        loan.setLateReturnDate(null);
        loan.setTotal(0);
        loan.setLateFine(0);
        loan.setDamagePenalty(0);

        // kardex movement
        UserEntity kardexUser = new UserEntity();
        kardexUser.setRut(customer.getRut());

        // Evitar repetidos en el mismo préstamo
        Set<Long> seen = new HashSet<>();

        for (Item it : items) {
            if (it == null || it.toolId == null)
                throw new IllegalArgumentException("Each item requires 'toolId'.");

            if (!seen.add(it.toolId))
                throw new IllegalArgumentException("Tool repeated in the same loan: " + it.toolId);

            int qty = (it.quantity == null) ? 1 : it.quantity;
            if (qty <= 0) throw new IllegalArgumentException("quantity must be >= 1");

            ToolEntity tool = toolRepository.findById(it.toolId)
                    .orElseThrow(() -> new IllegalArgumentException("Tool not found (id=" + it.toolId + ")"));

            // aviable and stock
            if (!"Disponible".equalsIgnoreCase(tool.getInitialState()))
                throw new IllegalArgumentException("Tool id=" + it.toolId + " is not 'Disponible'.");
            if (tool.getAmount() < qty)
                throw new IllegalArgumentException("Not enough stock for tool id=" + it.toolId +
                        ". Available: " + tool.getAmount());

            //only one tool per client
            if (qty != 1) throw new IllegalArgumentException("Only one unit per tool is allowed.");
            boolean alreadyActive = loanRepository
                    .existsByRutUserAndLateReturnDateIsNullAndItems_Tool_Id(rutUser, it.toolId);
            if (alreadyActive)
                throw new IllegalArgumentException("User already has this tool in an active loan: " + it.toolId);

            // Reservar stock / kardex
            for (int i = 0; i < qty; i++) {
                toolService.updateTool(it.toolId, "Prestada", null, kardexUser);
            }


            LoanItemEntity line = new LoanItemEntity();
            line.setTool(tool);
            line.setToolNameSnapshot(tool.getName());

            loan.addItem(line);
        }

        // amountOfTools
        return loanRepository.save(loan);
    }
    
    @Transactional
    public LoanEntity returnLoan(
            Long loanId,
            LocalDate actualReturnDate,
            Set<Long> damagedToolIds,      // opcional
            Set<Long> irreparableToolIds,  // opcional
            Integer finePerDay
    ) {
        if (actualReturnDate == null) throw new IllegalArgumentException("actualReturnDate is required.");

        LoanEntity loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + loanId));

        if (loan.getLateReturnDate() != null)
            throw new IllegalArgumentException("Loan is already returned (closed).");

        // Asegurar que los sets no sean null
        damagedToolIds = (damagedToolIds == null) ? Collections.emptySet() : damagedToolIds;
        irreparableToolIds = (irreparableToolIds == null) ? Collections.emptySet() : irreparableToolIds;

        // No pueden superponerse
        Set<Long> inter = new HashSet<>(damagedToolIds);
        inter.retainAll(irreparableToolIds);
        if (!inter.isEmpty())
            throw new IllegalArgumentException("A tool cannot be both damaged and irreparable: " + inter);

        // Validar que los IDs correspondan a herramientas del préstamo
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

        // Para kardex: usamos el rut del cliente
        UserEntity kardexUser = new UserEntity();
        kardexUser.setRut(loan.getRutUser());

        int damagePenalty = 0;

        for (LoanItemEntity line : loan.getItems()) {
            ToolEntity tool = line.getTool();
            Long id = tool.getId();

            if (irreparableToolIds.contains(id)) {
                // Baja definitiva + reposición
                Integer replacement = tool.getRepositionValue(); // ajusta el getter si tu entidad usa otro nombre
                if (replacement == null) replacement = 0;
                damagePenalty += replacement;
                toolService.updateTool(id, "Dada de baja", null, kardexUser);
            } else if (damagedToolIds.contains(id)) {
                // En reparación
                toolService.updateTool(id, "En reparación", null, kardexUser);
            } else {
                // OK → Disponible
                toolService.updateTool(id, "Disponible", null, kardexUser);
            }
        }

        // Multa por atraso
        int fineRate = (finePerDay == null) ? 0 : Math.max(0, finePerDay);
        long lateDays = Math.max(0, ChronoUnit.DAYS.between(loan.getReturnDate(), actualReturnDate));
        int lateFine = (int) (lateDays * (long) fineRate);

        loan.setLateReturnDate(actualReturnDate);
        loan.setLateFine(lateFine);
        loan.setDamagePenalty(damagePenalty);

        return loanRepository.save(loan);
    }



    public static class Item {
        public Long toolId;
        public Integer quantity;
        public Item() {}
    }
}
