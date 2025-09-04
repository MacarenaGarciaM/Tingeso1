package com.example.demo.services;

import com.example.demo.entities.KardexEntity;
import com.example.demo.entities.ToolEntity;
import com.example.demo.repositories.KardexRepository;
import com.example.demo.repositories.ToolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
public class ToolService {

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private KardexRepository kardexRepository;



    private static final List<String> validState =
            Arrays.asList("Disponible", "Prestada", "En reparación", "Dada de baja");

    public ToolEntity saveTool(ToolEntity tool) {
        // Validaciones
        if (tool.getName() == null || tool.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tool name is required.");
        }
        if (tool.getCategory() == null || tool.getCategory().trim().isEmpty()) {
            throw new IllegalArgumentException("Tool category is required.");
        }
        if (tool.getRepositionValue() <= 0) {
            throw new IllegalArgumentException("Reposition value must be greater than 0.");
        }
        if (tool.getInitialState() == null || tool.getInitialState().trim().isEmpty()) {
            throw new IllegalArgumentException("Initial state is required.");
        }
        if (!validState.contains(tool.getInitialState())) {
            throw new IllegalArgumentException("Initial state is not valid.");
        }

        // create the new tool
        ToolEntity newTool = new ToolEntity(
                null,
                tool.getName(),
                tool.getCategory(),
                tool.getInitialState(),
                tool.getRepositionValue(),
                tool.isAvailable(),
                tool.getAmount()
        );
        ToolEntity savedTool = toolRepository.save(newTool);

        // Register tool in kardex
        KardexEntity kardex = new KardexEntity();
        kardex.setTool(savedTool);
        kardex.setRutUser("ADMIN"); // Aquí debería ir el usuario autenticado
        kardex.setType("Ingreso");
        kardex.setMovementDate(LocalDate.now());
        kardex.setStock(savedTool.getAmount());
        kardexRepository.save(kardex);

        return savedTool;
    }

    public ToolEntity updateTool(Long id, String newState, Integer newAmount) {
        ToolEntity tool = toolRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found"));

        boolean stockChanged = false;

        if (newState != null) {
            if (!validState.contains(newState)) {
                throw new IllegalArgumentException("Invalid state: " + newState);
            }
            tool.setInitialState(newState);
            tool.setAvailable(newState.equals("Disponible"));
        }

        if (newAmount != null) {
            if (newAmount < 0) {
                throw new IllegalArgumentException("Amount cannot be negative.");
            }
            if (!newAmount.equals(tool.getAmount())) {
                stockChanged = true;
            }
            tool.setAmount(newAmount);
        }

        ToolEntity updatedTool = toolRepository.save(tool);

        //Register movement in kardex IF the stock was changed
        if (stockChanged) {
            KardexEntity kardex = new KardexEntity();
            kardex.setTool(updatedTool);
            kardex.setRutUser("ADMIN"); // Aquí debería ir el usuario autenticado
            kardex.setType("Actualización Stock");
            kardex.setMovementDate(LocalDate.now());
            kardex.setStock(updatedTool.getAmount());
            kardexRepository.save(kardex);
        }

        return updatedTool;
    }
}
