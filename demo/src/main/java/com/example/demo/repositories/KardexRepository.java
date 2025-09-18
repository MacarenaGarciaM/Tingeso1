package com.example.demo.repositories;

import com.example.demo.entities.KardexEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface KardexRepository extends JpaRepository<KardexEntity, Long> {



}
