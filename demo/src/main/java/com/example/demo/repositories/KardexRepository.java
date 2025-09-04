package com.example.demo.repositories;

import com.example.demo.entities.KardexEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface KardexRepository extends JpaRepository<KardexEntity, Long> {
    List<KardexEntity> findByRutUser(String rutUser);
    List<KardexEntity> findByMovementDate(LocalDate movementDate);
    List<KardexEntity> findByStock(int stock);
}
