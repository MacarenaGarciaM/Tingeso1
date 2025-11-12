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
    and (:typeLower = '' or lower(k.type) = :typeLower)
    and (:hasFrom = false or k.movementDate >= :fromDate)
    and (:hasTo   = false or k.movementDate <= :toDate)
    and (:namePat = '' or lower(k.tool.name) like :namePat)
    and (:categoryPat = '' or lower(k.tool.category) like :categoryPat)
""")
    Page<KardexEntity> search(
            @Param("toolId") Long toolId,
            @Param("rutUser") String rutUser,
            @Param("typeLower") String typeLower,     // "" if doesn't filter by type
            @Param("hasFrom") boolean hasFrom,        // true if start != null
            @Param("fromDate") LocalDate fromDate,    // can be null
            @Param("hasTo") boolean hasTo,            // true if end != null
            @Param("toDate") LocalDate toDate,        // can be null
            @Param("namePat") String namePat,         // "" if doesn't filter by name
            @Param("categoryPat") String categoryPat, // "" if doesn't filter by category
            Pageable pageable
    );

}


