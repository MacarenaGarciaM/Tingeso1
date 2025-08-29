package com.example.demo.repositories;

import com.example.demo.entities.LoanEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<LoanEntity, Long> {
    List<LoanEntity> findByRutUser(String rutUser);
    List<LoanEntity> findByReturnDate(LocalDate returnDate);

}
