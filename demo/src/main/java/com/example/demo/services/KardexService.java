// src/main/java/com/example/demo/services/KardexService.java
package com.example.demo.services;

import com.example.demo.entities.KardexEntity;
import com.example.demo.repositories.KardexRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class KardexService {
    private final KardexRepository kardexRepository;
    // src/main/java/com/example/demo/services/KardexService.java
    public Page<KardexEntity> search(
            Long toolId, String rutUser, String type,
            LocalDate start, LocalDate end,
            String name, String category,
            Pageable pageable
    ) {
        String typeLower = (type == null || type.isBlank()) ? null : type.toLowerCase();

        String namePat = (name == null || name.isBlank())
                ? null
                : "%" + name.toLowerCase() + "%";

        String categoryPat = (category == null || category.isBlank())
                ? null
                : "%" + category.toLowerCase() + "%";

        return kardexRepository.search(
                toolId, rutUser, typeLower, start, end, namePat, categoryPat, pageable
        );
    }

}
