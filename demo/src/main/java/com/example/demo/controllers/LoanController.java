package com.example.demo.controllers;

import com.example.demo.entities.LoanEntity;
import com.example.demo.repositories.LoanRepository;
import com.example.demo.services.LoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @Autowired
    private LoanItemRepository loanItemRepository;
    @Autowired
    private LoanRepository loanRepository;

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
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

    @PreAuthorize("hasAnyRole('ADMIN')")
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

            Map<Long, Integer> damagedCosts = new HashMap<>();
            Object dc = body.get("damagedCosts");
            if (dc instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    Long key = Long.valueOf(e.getKey().toString());
                    Integer val = (e.getValue() == null) ? 0 :
                            (e.getValue() instanceof Number n ? n.intValue() : Integer.valueOf(e.getValue().toString()));
                    damagedCosts.put(key, Math.max(0, val));
                }
            }

            LoanEntity updated = loanService.returnLoan(
                    loanId, actualReturnDate, damaged, irreparable, finePerDay, damagedCosts
            );

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("id", updated.getId());
            out.put("lateFine", updated.getLateFine());
            out.put("damagePenalty", updated.getDamagePenalty());
            return ResponseEntity.ok(out);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(ex.getMessage());
        }
    }



    //PayFines--> user active again
    @PreAuthorize("hasAnyRole('ADMIN')")
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

    //Helpers:

    //Accepts "YYYY-MM-DD" or ISO datetime
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


    //Turns arrays or loose values to Set<Long>
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

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping(value="/active", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<LoanEntity>> listActive(
            @RequestParam(required = false) String rutUser,
            org.springframework.security.core.Authentication auth
    ) {
        boolean isAdmin = auth.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .anyMatch(a -> a != null && a.equalsIgnoreCase("ROLE_ADMIN")); // 👈

        if (rutUser == null || rutUser.isBlank()) {
            if (isAdmin) {
                return ResponseEntity.ok(loanService.listAllActiveLoans());
            } else {
                return ResponseEntity.badRequest().build();
            }
        }
        return ResponseEntity.ok(loanService.listActiveLoans(rutUser));
    }


    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/top")
    public ResponseEntity<?> topTools(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        int size = (limit == null || limit <= 0) ? 10 : limit;

        boolean hasStart = (start != null);
        boolean hasEnd   = (end   != null);

        List<Object[]> rows = loanItemRepository.topByToolName(
                hasStart, start, hasEnd, end, PageRequest.of(0, size)
        );

        List<Map<String, Object>> out = new java.util.ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("tool",  (String) r[0]);
            m.put("times", ((Number) r[1]).longValue());
            out.add(m);
        }
        return ResponseEntity.ok(out);
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @GetMapping("/debts")
    public ResponseEntity<Page<LoanEntity>> listLoansWithDebts(
            @RequestParam(required = false) String rutUser,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "reservationDate,desc") String sort
    ) {
        String[] s = sort.split(",", 2);
        Sort.Direction dir = (s.length > 1 && "asc".equalsIgnoreCase(s[1])) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortObj = Sort.by(dir, s[0]);

        PageRequest pr = PageRequest.of(Math.max(page,0), Math.max(size,1), sortObj);
        return ResponseEntity.ok(loanService.listLoansWithUnpaidDebts(
                (rutUser != null && rutUser.isBlank()) ? null : rutUser,
                start, end, pr
        ));
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/by-rut")
    public ResponseEntity<?> listByRut(
            @RequestParam String rutUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "reservationDate,desc") String sort
    ) {
        String[] s = sort.split(",", 2);
        Sort.Direction dir = (s.length > 1 && "asc".equalsIgnoreCase(s[1])) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pr = PageRequest.of(Math.max(page,0), Math.max(size,1), Sort.by(dir, s[0]));

        return ResponseEntity.ok(loanRepository.findPageByRutUser(rutUser, pr));
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/overdue")
    public ResponseEntity<Page<LoanEntity>> listOverdue(
            @RequestParam(required = false) String rutUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "returnDate,asc") String sort
    ) {
        String[] s = sort.split(",", 2);
        Sort.Direction dir = (s.length > 1 && "desc".equalsIgnoreCase(s[1])) ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pr = PageRequest.of(Math.max(page,0), Math.max(size,1), Sort.by(dir, s[0]));
        return ResponseEntity.ok(loanService.listOverdueLoans(
                (rutUser != null && rutUser.isBlank()) ? null : rutUser, pr));
    }
}