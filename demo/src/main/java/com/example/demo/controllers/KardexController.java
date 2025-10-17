// src/main/java/com/example/demo/controllers/KardexController.java
package com.example.demo.controllers;

import com.example.demo.entities.KardexEntity;
import com.example.demo.services.KardexService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/kardex")
@CrossOrigin("*")
@RequiredArgsConstructor
public class KardexController {

    private final KardexService kardexService;

    @PreAuthorize("hasAnyRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<KardexEntity>> list(
            @RequestParam(required = false) Long toolId,
            @RequestParam(required = false) String rutUser,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "movementDate,desc") String sort
    ) {
        // Normaliza strings vacíos a null para que los filtros opcionales funcionen


        rutUser = (rutUser != null && rutUser.isBlank()) ? null : rutUser;
        type = (type != null && type.isBlank()) ? null : type;
        name = (name != null && name.isBlank()) ? null : name;
        category = (category != null && category.isBlank()) ? null : category;

        // Sort: "campo,dir"
        String[] s = sort.split(",", 2);
        Sort.Direction dir = (s.length > 1 && "asc".equalsIgnoreCase(s[1])) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortObj = Sort.by(dir, s[0]);
        PageRequest pr = PageRequest.of(Math.max(page,0), Math.max(size,1), sortObj);

        Page<KardexEntity> out = kardexService.search(toolId, rutUser, type, start, end, name, category, pr);
        return ResponseEntity.ok(out);
    }
}
