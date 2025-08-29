package com.example.demo.repositories;

import com.example.demo.entities.kardexEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface KardexRepository extends JpaRepository<kardexEntity, Long> {
    List<kardexEntity> findByRutUser(String rutUser);
    List<kardexEntity> findByReturnDate(LocalDate returnDate);
    List<kardexEntity> findByStock(int stock);
}
