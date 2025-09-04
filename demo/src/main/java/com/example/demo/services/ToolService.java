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
import java.util.Optional;

@Service
public class ToolService {

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private KardexRepository kardexRepository;

    private static final List<String> validState =
            Arrays.asList("Disponible", "Prestada", "En reparación", "Dada de baja");

    public ToolEntity saveTool(ToolEntity tool) {
        // Validaciones básicas
        if (tool.getName() == null || tool.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tool name is required.");
        }
        if (tool.getCategory() == null || tool.getCategory().trim().isEmpty()) {
            throw new IllegalArgumentException("Tool category is required.");
        }
        if (tool.getRepositionValue() <= 0) {
            throw new IllegalArgumentException("Reposition value must be greater than 0.");
        }
        if (tool.getAmount() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0.");
        }
        if (tool.getInitialState() == null || tool.getInitialState().trim().isEmpty()) {
            throw new IllegalArgumentException("Initial state is required.");
        }
        if (!validState.contains(tool.getInitialState())) {
            throw new IllegalArgumentException("Initial state is not valid.");
        }

        // Buscar si ya existe una herramienta con el mismo nombre y categoría
        List<ToolEntity> existingToolOpt = toolRepository.findByNameAndCategory(tool.getName(), tool.getCategory());

        List<ToolEntity> existingTools = toolRepository.findByNameAndCategory(tool.getName(), tool.getCategory());

        ToolEntity savedTool;
        if (!existingTools.isEmpty()) {
            // Tomar la primera coincidencia (o definir qué hacer si hay más)
            ToolEntity existingTool = existingTools.get(0);
            int newAmount = existingTool.getAmount() + tool.getAmount();
            existingTool.setAmount(newAmount);
            existingTool.setInitialState(tool.getInitialState());
            existingTool.setAvailable(tool.isAvailable());
            existingTool.setRepositionValue(tool.getRepositionValue());
            savedTool = toolRepository.save(existingTool);
        } else {
            ToolEntity newTool = new ToolEntity(
                    null,
                    tool.getName(),
                    tool.getCategory(),
                    tool.getInitialState(),
                    tool.getRepositionValue(),
                    tool.isAvailable(),
                    tool.getAmount()
            );
            savedTool = toolRepository.save(newTool);
        }

        // Calcular stock acumulado de la categoría (no solo la herramienta)
        int currentStock = kardexRepository.getCurrentStockByCategory(savedTool.getCategory());
        int updatedStock = currentStock + tool.getAmount();

        // Registrar movimiento en kardex
        KardexEntity kardex = new KardexEntity();
        kardex.setTool(savedTool);
        kardex.setRutUser("ADMIN"); // Usuario autenticado aquí
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

        if (stockChanged) {
            // recalcular stock acumulado de la categoría
            int currentStock = kardexRepository.getCurrentStockByCategory(updatedTool.getCategory());
            KardexEntity kardex = new KardexEntity();
            kardex.setTool(updatedTool);
            kardex.setRutUser("ADMIN");
            kardex.setType("Actualización Stock");
            kardex.setMovementDate(LocalDate.now());
            kardex.setStock(currentStock);
            kardexRepository.save(kardex);
        }

        return updatedTool;
    }
}
