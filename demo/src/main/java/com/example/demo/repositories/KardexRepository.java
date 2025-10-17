// src/main/java/com/example/demo/repositories/KardexRepository.java
package com.example.demo.repositories;

import com.example.demo.entities.KardexEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface KardexRepository extends JpaRepository<KardexEntity, Long> {

    @EntityGraph(attributePaths = {"tool"})
    @Query("""
  select k
  from KardexEntity k
  where (:toolId is null or k.tool.id = :toolId)
    and (:rutUser is null or k.rutUser = :rutUser)
    and (:typeLower is null or lower(k.type) = :typeLower)
    and (:start is null or k.movementDate >= :start)
    and (:end is null or k.movementDate <= :end)
    and (:namePat is null or lower(k.tool.name) like :namePat)
    and (:categoryPat is null or lower(k.tool.category) like :categoryPat)
""")
    Page<KardexEntity> search(
            @Param("toolId") Long toolId,
            @Param("rutUser") String rutUser,
            @Param("typeLower") String typeLower,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("namePat") String namePat,
            @Param("categoryPat") String categoryPat,
            Pageable pageable
    );
}
