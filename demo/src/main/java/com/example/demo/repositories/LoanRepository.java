// src/main/java/com/example/demo/repositories/LoanRepository.java
package com.example.demo.repositories;

import com.example.demo.entities.LoanEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<LoanEntity, Long> {

    long countByRutUserAndLateReturnDateIsNull(String rutUser);
    Page<LoanEntity> findByRutUserOrderByReservationDateDesc(String rutUser, Pageable pageable);

    @EntityGraph(attributePaths = {"items", "items.tool"})
    List<LoanEntity> findByLateReturnDateIsNull();

    @EntityGraph(attributePaths = {"items", "items.tool"})
    List<LoanEntity> findByRutUserAndLateReturnDateIsNull(String rutUser);

    @EntityGraph(attributePaths = {"items", "items.tool"})
    Page<LoanEntity> findPageByRutUser(String rutUser, Pageable pageable);

    boolean existsByRutUserAndLateReturnDateIsNullAndItems_Tool_Id(String rutUser, Long toolId);
    boolean existsByRutUserAndReturnDateBeforeAndLateReturnDateIsNull(String rutUser, LocalDate today);
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


    @EntityGraph(attributePaths = {"items","items.tool"})
    @Query("""
  select l
  from LoanEntity l
  where
    (
      (l.lateFine > 0 and (l.lateFinePaid = false or l.lateFinePaid is null))
      or
      (l.damagePenalty > 0 and (l.damagePenaltyPaid = false or l.damagePenaltyPaid is null))
    )
    and (:rut is null or l.rutUser = :rut)
    and (:hasStart = false or l.reservationDate >= :start)
    and (:hasEnd   = false or l.reservationDate <= :end)
""")
    Page<LoanEntity> findLoansWithUnpaidDebts(
            @Param("rut") String rutUser,
            @Param("hasStart") boolean hasStart,
            @Param("start") LocalDate start,
            @Param("hasEnd") boolean hasEnd,
            @Param("end") LocalDate end,
            Pageable pageable
    );






    @Override
    @EntityGraph(attributePaths = {"items", "items.tool"})
    Optional<LoanEntity> findById(Long id);
}