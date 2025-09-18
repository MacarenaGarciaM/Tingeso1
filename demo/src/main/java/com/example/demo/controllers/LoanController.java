// src/main/java/com/example/demo/controllers/LoanController.java
package com.example.demo.controllers;

import com.example.demo.entities.LoanEntity;
import com.example.demo.services.LoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@RestController
@RequestMapping("/loan")
@CrossOrigin("*")
public class LoanController {

    @Autowired
    private LoanService loanService;

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

    @PostMapping("/{loanId}/return")
    public ResponseEntity<?> returnLoan(
            @PathVariable Long loanId,
            @RequestBody Map<String, Object> body
    ) {
        try {
            if (body == null) {
                return ResponseEntity.badRequest().body("Body is required.");
            }

            // actualReturnDate (obligatoria)
            Object actualReturnDateObj = body.get("actualReturnDate");
            if (actualReturnDateObj == null) {
                return ResponseEntity.badRequest().body("Field 'actualReturnDate' is required (YYYY-MM-DD).");
            }
            LocalDate actualReturnDate = parseDateFlex(actualReturnDateObj.toString());

            // finePerDay (opcional)
            Integer finePerDay = null;
            Object finePerDayObj = body.get("finePerDay");
            if (finePerDayObj != null) {
                if (finePerDayObj instanceof Number n) finePerDay = n.intValue();
                else finePerDay = Integer.valueOf(finePerDayObj.toString());
            }

            // damaged / irreparable (opcionales) → Set<Long>
            Set<Long> damaged = toIdSet(body.get("damaged"));
            Set<Long> irreparable = toIdSet(body.get("irreparable"));

            LoanEntity updated = loanService.returnLoan(
                    loanId,
                    actualReturnDate,
                    damaged,
                    irreparable,
                    finePerDay
            );
            return ResponseEntity.ok(updated);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /** Acepta "YYYY-MM-DD" o un ISO datetime (toma solo la fecha) */
    private LocalDate parseDateFlex(String raw) {
        try {
            return LocalDate.parse(raw); // YYYY-MM-DD
        } catch (DateTimeParseException ignore) {
            try {
                return OffsetDateTime.parse(raw).toLocalDate(); // ISO datetime
            } catch (DateTimeParseException e2) {
                throw new IllegalArgumentException("Invalid date: " + raw + ". Use YYYY-MM-DD.");
            }
        }
    }

    /**
     * Convierte el valor del body a Set<Long>.
     * Acepta: [1,2,3] o ["1","2"] o un único número/str, o null.
     */
    @SuppressWarnings("unchecked")
    private Set<Long> toIdSet(Object value) {
        if (value == null) return Collections.emptySet();

        Set<Long> out = new HashSet<>();

        if (value instanceof List<?> list) {
            for (Object el : list) {
                if (el == null) continue;
                if (el instanceof Number n) out.add(n.longValue());
                else out.add(Long.valueOf(el.toString()));
            }
            return out;
        }

        // Si mandaron un solo número/str en lugar de array
        if (value instanceof Number n) {
            out.add(n.longValue());
            return out;
        }
        out.add(Long.valueOf(value.toString()));
        return out;
    }
}

