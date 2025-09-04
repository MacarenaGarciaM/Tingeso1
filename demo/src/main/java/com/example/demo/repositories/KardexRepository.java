package com.example.demo.repositories;

import com.example.demo.entities.KardexEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface KardexRepository extends JpaRepository<KardexEntity, Long> {
    List<KardexEntity> findByRutUser(String rutUser);
    List<KardexEntity> findByMovementDate(LocalDate movementDate);
    List<KardexEntity> findByStock(int stock);
    @Query("SELECT COALESCE(SUM(k.stock), 0) " +
            "FROM KardexEntity k " +
            "WHERE k.tool.category = :category")
    int getCurrentStockByCategory(@Param("category") String category);


}
