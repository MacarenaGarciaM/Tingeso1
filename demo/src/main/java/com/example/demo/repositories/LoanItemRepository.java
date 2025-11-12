package com.example.demo.repositories;

import com.example.demo.entities.LoanItemEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LoanItemRepository extends JpaRepository<LoanItemEntity, Long> {

    //Ranking by name, "snapshot" saved in loan_item (avoids duplicate states buckets )
    @Query("""
        select li.toolNameSnapshot as tool, count(li) as times
        from LoanItemEntity li
        join li.loan l
        where (:hasStart = false or l.reservationDate >= :start)
          and (:hasEnd = false or l.reservationDate <= :end)
        group by li.toolNameSnapshot
        order by times desc
    """)
    List<Object[]> topByToolName(
            @Param("hasStart") boolean hasStart,
            @Param("start") LocalDate start,
            @Param("hasEnd") boolean hasEnd,
            @Param("end") LocalDate end,
            Pageable pageable
    );
}
