// src/main/java/com/example/demo/controllers/LoanController.java
package com.example.demo.controllers;

import com.example.demo.entities.LoanEntity;
import com.example.demo.services.LoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/loan")
@CrossOrigin("*")
public class LoanController {

    @Autowired
    private LoanService loanService;

    /**
     * Crea UN préstamo con múltiples herramientas.
     * - Query params: rutUser, reservationDate, returnDate
     * - Body: JSON array con los items [{ "toolId": 1, "quantity": 1 }, ...]
     *
     * Ejemplo curl:
     * curl -X POST "http://localhost:8080/loan?rutUser=12.345.678-9&reservationDate=2025-09-13&returnDate=2025-09-20" \
     *   -H "Content-Type: application/json" \
     *   -d '[{"toolId":1,"quantity":1},{"toolId":3,"quantity":1}]'
     */
    @PostMapping
    public ResponseEntity<?> createLoan(
            @RequestParam String rutUser,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reservationDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate returnDate,
            @RequestBody List<LoanService.Item> items
    ) {
        try {
            LoanEntity loan = loanService.createLoan(rutUser, reservationDate, returnDate, items);
            return ResponseEntity.ok(loan);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
