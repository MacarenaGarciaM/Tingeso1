// src/main/java/com/example/demo/repositories/LoanItemRepository.java
package com.example.demo.repositories;

import com.example.demo.entities.LoanItemEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface LoanItemRepository extends JpaRepository<LoanItemEntity, Long> {

    // Ranking por nombre "snapshot" guardado en loan_item (evita duplicar por buckets de estado)
    @Query("""
        select li.toolNameSnapshot, count(li.id)
        from LoanItemEntity li
        join li.loan l
        where (:start is null or l.reservationDate >= :start)
          and (:end   is null or l.reservationDate <= :end)
        group by li.toolNameSnapshot
        order by count(li.id) desc
    """)
    List<Object[]> topByToolName(LocalDate start, LocalDate end, Pageable pageable);
}
