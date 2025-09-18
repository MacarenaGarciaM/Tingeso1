package com.example.demo.repositories;

import com.example.demo.entities.LoanEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LoanRepository extends JpaRepository<LoanEntity, Long> {

    // Límite de 5 préstamos activos por usuario
    long countByRutUserAndLateReturnDateIsNull(String rutUser);

    // Verificar si el usuario ya tiene en un préstamo activo la herramienta X
    boolean existsByRutUserAndLateReturnDateIsNullAndItems_Tool_Id(String rutUser, Long toolId);
}
