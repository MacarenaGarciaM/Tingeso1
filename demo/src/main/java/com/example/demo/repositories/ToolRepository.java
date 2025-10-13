package com.example.demo.repositories;


import com.example.demo.entities.ToolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ToolRepository extends JpaRepository<ToolEntity, Long> {
    List<ToolEntity> findByNameAndCategory(String name, String category);
    List<ToolEntity> findByName(String name);
    List<ToolEntity> findAllByInitialStateIgnoreCaseAndAmountGreaterThan(String initialState, int amount);
}
