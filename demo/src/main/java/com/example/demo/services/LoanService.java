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
import java.util.*;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final ToolRepository toolRepository;
    private final UserRepository userRepository;
    private final ToolService toolService; // asumes que ya existe en tu proyecto

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

        // Regla: máx. 5 préstamos activos
        long activeCount = loanRepository.countByRutUserAndLateReturnDateIsNull(rutUser);
        if (activeCount >= 5)
            throw new IllegalArgumentException("User already has 5 active loans.");

        // Cabecera
        LoanEntity loan = new LoanEntity();
        loan.setRutUser(customer.getRut());
        loan.setReservationDate(reservationDate);
        loan.setReturnDate(returnDate);
        loan.setLateReturnDate(null);
        loan.setTotal(0);
        loan.setLateFine(0);
        loan.setDamagePenalty(0);

        // Para kardex
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

            // Disponible y con stock
            if (!"Disponible".equalsIgnoreCase(tool.getInitialState()))
                throw new IllegalArgumentException("Tool id=" + it.toolId + " is not 'Disponible'.");
            if (tool.getAmount() < qty)
                throw new IllegalArgumentException("Not enough stock for tool id=" + it.toolId +
                        ". Available: " + tool.getAmount());

            // Regla: no más de una unidad de la misma herramienta activa por cliente
            if (qty != 1) throw new IllegalArgumentException("Only one unit per tool is allowed.");
            boolean alreadyActive = loanRepository
                    .existsByRutUserAndLateReturnDateIsNullAndItems_Tool_Id(rutUser, it.toolId);
            if (alreadyActive)
                throw new IllegalArgumentException("User already has this tool in an active loan: " + it.toolId);

            // Reservar stock / kardex
            for (int i = 0; i < qty; i++) {
                toolService.updateTool(it.toolId, "Prestada", null, kardexUser);
            }

            // Crear línea
            LoanItemEntity line = new LoanItemEntity();
            line.setTool(tool);
            line.setToolNameSnapshot(tool.getName());
            line.setQuantity(qty);

            loan.addItem(line);
        }

        // amountOfTools = tamaño de items (ya lo actualiza addItem)
        return loanRepository.save(loan);
    }

    // ====== Tipo para el body (sin DTOs externos) ======
    public static class Item {
        public Long toolId;
        public Integer quantity;
        public Item() {}
    }
}
