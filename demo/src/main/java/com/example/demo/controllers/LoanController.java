// src/main/java/com/example/demo/controllers/LoanController.java
package com.example.demo.controllers;

import com.example.demo.entities.LoanEntity;
import com.example.demo.services.LoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.demo.repositories.LoanItemRepository;
import org.springframework.data.domain.PageRequest;

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

    // Crear préstamo (body = array de items)
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

    // Devolución (body con actualReturnDate, finePerDay y listas damaged/irreparable)
    @PostMapping("/{loanId}/return")
    public ResponseEntity<?> returnLoan(
            @PathVariable Long loanId,
            @RequestBody Map<String, Object> body
    ) {
        try {
            if (body == null) return ResponseEntity.badRequest().body("Body is required.");

            Object actualReturnDateObj = body.get("actualReturnDate");
            if (actualReturnDateObj == null)
                return ResponseEntity.badRequest().body("Field 'actualReturnDate' is required (YYYY-MM-DD).");
            LocalDate actualReturnDate = parseDateFlex(actualReturnDateObj.toString());

            Integer finePerDay = null;
            Object finePerDayObj = body.get("finePerDay");
            if (finePerDayObj != null) {
                if (finePerDayObj instanceof Number n) finePerDay = n.intValue();
                else finePerDay = Integer.valueOf(finePerDayObj.toString());
            }

            Set<Long> damaged = toIdSet(body.get("damaged"));
            Set<Long> irreparable = toIdSet(body.get("irreparable"));

            LoanEntity updated = loanService.returnLoan(loanId, actualReturnDate, damaged, irreparable, finePerDay);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // Pagar multas (para que el usuario pueda volver a activo)
    @PostMapping("/{loanId}/pay-fines")
    public ResponseEntity<?> payFines(
            @PathVariable Long loanId,
            @RequestBody Map<String, Object> body
    ) {
        try {
            boolean payLateFine = body.get("payLateFine") != null
                    && Boolean.parseBoolean(body.get("payLateFine").toString());
            boolean payDamagePenalty = body.get("payDamagePenalty") != null
                    && Boolean.parseBoolean(body.get("payDamagePenalty").toString());

            return ResponseEntity.ok(loanService.payFines(loanId, payLateFine, payDamagePenalty));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // ===== Helpers =====

    /** Acepta "YYYY-MM-DD" o ISO datetime y toma la fecha */
    private LocalDate parseDateFlex(String raw) {
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException ignore) {
            try {
                return OffsetDateTime.parse(raw).toLocalDate();
            } catch (DateTimeParseException e2) {
                throw new IllegalArgumentException("Invalid date: " + raw + ". Use YYYY-MM-DD.");
            }
        }
    }

    /** Convierte arrays o valores sueltos a Set<Long> */
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
        if (value instanceof Number n) { out.add(n.longValue()); return out; }
        out.add(Long.valueOf(value.toString()));
        return out;
    }

    @Autowired
    private LoanItemRepository loanItemRepository;

    /**
     * Ranking simple de herramientas más prestadas.
     * Ej: GET /loan/top?limit=5&start=2025-09-01&end=2025-09-30
     * Todos los parámetros son opcionales.
     */
    @GetMapping("/top")
    public ResponseEntity<?> topTools(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        int size = (limit == null || limit <= 0) ? 10 : limit;

        List<Object[]> rows = loanItemRepository.topByToolName(start, end, PageRequest.of(0, size));

        // Mapear Object[] -> { "tool": String, "times": long }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("tool",  r[0]);            // toolNameSnapshot
            m.put("times", ((Number) r[1]).longValue()); // count
            out.add(m);
        }
        return ResponseEntity.ok(out);
    }
}
