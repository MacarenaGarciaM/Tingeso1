package com.example.demo.repositories;


import com.example.demo.entities.ToolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface ToolRepository extends JpaRepository<ToolEntity, Long> {
    List<ToolEntity> findByNameAndCategory(String name, String category);
    List<ToolEntity> findByName(String name);
    List<ToolEntity> findAllByInitialStateIgnoreCaseAndAmountGreaterThan(String initialState, int amount);

    //Sin query me sale un error extraño :(
    @Query("""
      select t.id
      from ToolEntity t
      where lower(t.name) = lower(:name)
        and lower(t.category) = lower(:category)
        and lower(t.initialState) = lower(:state)
    """)
    List<Long> findIdsByNameCategoryAndState(
            @Param("name") String name,
            @Param("category") String category,
            @Param("state") String state
    );

    List<ToolEntity> findAllByInitialStateIgnoreCase(String state);
    Optional<ToolEntity> findFirstByNameAndCategoryAndInitialState(String name, String category, String initialState);
    List<ToolEntity> findByNameAndCategoryAndInitialState(String name, String category, String initialState);


}
