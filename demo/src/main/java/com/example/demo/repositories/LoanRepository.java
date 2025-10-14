// src/main/java/com/example/demo/repositories/LoanRepository.java
package com.example.demo.repositories;

import com.example.demo.entities.LoanEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface LoanRepository extends JpaRepository<LoanEntity, Long> {

    long countByRutUserAndLateReturnDateIsNull(String rutUser);


    // Validar si el usuario ya tiene activa la misma herramienta (por tool base)
    boolean existsByRutUserAndLateReturnDateIsNullAndItems_Tool_Id(String rutUser, Long toolId);

    // ¿Tiene préstamos vencidos (pactada < hoy) sin devolver?
    boolean existsByRutUserAndReturnDateBeforeAndLateReturnDateIsNull(String rutUser, LocalDate today);

    // ¿Tiene multas impagas?
    boolean existsByRutUserAndLateFineGreaterThanAndLateFinePaidIsFalse(String rutUser, int min);
    boolean existsByRutUserAndDamagePenaltyGreaterThanAndDamagePenaltyPaidIsFalse(String rutUser, int min);
    @Query("""
      select case when count(li)>0 then true else false end
      from LoanEntity l
      join l.items li
      where l.rutUser = :rut
        and l.lateReturnDate is null
        and li.tool.id in :toolIds
    """)
    boolean existsActiveWithAnyToolId(
            @Param("rut") String rutUser,
            @Param("toolIds") Collection<Long> toolIds
    );
}