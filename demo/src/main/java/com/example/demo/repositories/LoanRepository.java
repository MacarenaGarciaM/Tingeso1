// src/main/java/com/example/demo/repositories/LoanRepository.java
package com.example.demo.repositories;

import com.example.demo.entities.LoanEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LoanRepository extends JpaRepository<LoanEntity, Long> {

    List<LoanEntity> findByReturnDate(LocalDate returnDate);
    List<LoanEntity> findByRutUser(String rutUser);
    List<LoanEntity> findByRutUserAndLateReturnDateIsNull(String rutUser);

    @EntityGraph(attributePaths = "items")
    List<LoanEntity> findByRutUserAndLateReturnDateIsNullOrderByReservationDateDesc(String rutUser);

    long countByRutUserAndLateReturnDateIsNull(String rutUser);

    // Validar si el usuario ya tiene activa la misma herramienta (por tool base)
    boolean existsByRutUserAndLateReturnDateIsNullAndItems_Tool_Id(String rutUser, Long toolId);

    // ¿Tiene préstamos abiertos?
    boolean existsByRutUserAndLateReturnDateIsNull(String rutUser);

    // ¿Tiene préstamos vencidos (pactada < hoy) sin devolver?
    boolean existsByRutUserAndReturnDateBeforeAndLateReturnDateIsNull(String rutUser, LocalDate today);

    // ¿Tiene multas impagas?
    boolean existsByRutUserAndLateFineGreaterThanAndLateFinePaidIsFalse(String rutUser, int min);
    boolean existsByRutUserAndDamagePenaltyGreaterThanAndDamagePenaltyPaidIsFalse(String rutUser, int min);
}
