package com.example.demo.services;

import com.example.demo.entities.KardexEntity;
import com.example.demo.entities.ToolEntity;
import com.example.demo.entities.UserEntity;
import com.example.demo.repositories.KardexRepository;
import com.example.demo.repositories.ToolRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class ToolService {

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private KardexRepository kardexRepository;

    private static final List<String> validState =
            Arrays.asList("Disponible", "Prestada", "En reparación", "Dada de baja");

    public ToolEntity saveTool(ToolEntity tool, UserEntity rutUser) {
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
        List<ToolEntity> existingTools = toolRepository.findByNameAndCategory(tool.getName(), tool.getCategory());

        ToolEntity savedTool;
        if (!existingTools.isEmpty()) {
            // Si existe: sumamos la cantidad
            ToolEntity existingTool = existingTools.get(0);
            int newAmount = existingTool.getAmount() + tool.getAmount();
            existingTool.setAmount(newAmount);
            existingTool.setInitialState(tool.getInitialState());
            existingTool.setAvailable(tool.isAvailable());
            existingTool.setRepositionValue(tool.getRepositionValue());
            savedTool = toolRepository.save(existingTool);
        } else {
            // Si no existe: crear nueva
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

        // Registrar movimiento en kardex (solo la cantidad ingresada, no el acumulado)
        KardexEntity kardex = new KardexEntity();
        kardex.setTool(savedTool);
        kardex.setRutUser(rutUser.getRut()); // Usuario autenticado aquí
        kardex.setType("Ingreso");
        kardex.setMovementDate(LocalDate.now());
        kardex.setStock(tool.getAmount()); // SOLO la cantidad ingresada
        kardexRepository.save(kardex);

        return savedTool;
    }

    public ToolEntity updateTool(Long id, String newState, Integer newAmount, UserEntity rutUser) {
        ToolEntity tool = toolRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found"));

        if (newState != null) {
            if (!validState.contains(newState)) {
                throw new IllegalArgumentException("Invalid state: " + newState);
            }

            // Caso 1: Disponible -> Otro estado
            if (tool.getInitialState().equals("Disponible") && !newState.equals("Disponible")) {
                if (tool.getAmount() <= 0) {
                    throw new IllegalArgumentException("No hay stock disponible para mover a otro estado.");
                }

                // Reducir en Disponible
                tool.setAmount(tool.getAmount() - 1);
                toolRepository.save(tool);

                // Buscar si ya existe herramienta con mismo estado
                List<ToolEntity> existingTools = toolRepository.findByNameAndCategory(tool.getName(), tool.getCategory());
                Optional<ToolEntity> sameStateTool = existingTools.stream()
                        .filter(t -> newState.equals(t.getInitialState()))
                        .findFirst();

                ToolEntity targetTool;
                if (sameStateTool.isPresent()) {
                    targetTool = sameStateTool.get();
                    targetTool.setAmount(targetTool.getAmount() + 1);
                } else {
                    targetTool = new ToolEntity(
                            null,
                            tool.getName(),
                            tool.getCategory(),
                            newState,
                            tool.getRepositionValue(),
                            false,
                            1
                    );
                }
                ToolEntity savedTargetTool = toolRepository.save(targetTool);

                // Kardex
                KardexEntity kardex = new KardexEntity();
                kardex.setTool(savedTargetTool);
                kardex.setRutUser(rutUser.getRut());
                kardex.setType("Cambio de estado: " + newState);
                kardex.setMovementDate(LocalDate.now());
                kardex.setStock(savedTargetTool.getAmount());
                kardexRepository.save(kardex);

                return savedTargetTool;
            }

            // Caso 2: Otro estado -> Disponible
            if (!tool.getInitialState().equals("Disponible") && newState.equals("Disponible")) {
                if (tool.getAmount() <= 0) {
                    throw new IllegalArgumentException("No hay stock en este estado para devolver a Disponible.");
                }

                // Reducir del estado actual
                tool.setAmount(tool.getAmount() - 1);
                toolRepository.save(tool);

                // Buscar la herramienta Disponible
                List<ToolEntity> existingTools = toolRepository.findByNameAndCategory(tool.getName(), tool.getCategory());
                Optional<ToolEntity> disponibleTool = existingTools.stream()
                        .filter(t -> "Disponible".equals(t.getInitialState()))
                        .findFirst();

                ToolEntity targetTool;
                if (disponibleTool.isPresent()) {
                    targetTool = disponibleTool.get();
                    targetTool.setAmount(targetTool.getAmount() + 1);
                } else {
                    targetTool = new ToolEntity(
                            null,
                            tool.getName(),
                            tool.getCategory(),
                            "Disponible",
                            tool.getRepositionValue(),
                            true,
                            1
                    );
                }
                ToolEntity savedTargetTool = toolRepository.save(targetTool);

                // Kardex
                KardexEntity kardex = new KardexEntity();
                kardex.setTool(savedTargetTool);
                kardex.setRutUser(rutUser.getRut());
                kardex.setType("Cambio de estado: Disponible");
                kardex.setMovementDate(LocalDate.now());
                kardex.setStock(savedTargetTool.getAmount());
                kardexRepository.save(kardex);

                return savedTargetTool;
            }

            // Caso 3: Actualización normal (ej: Prestada -> Reparación)
            tool.setInitialState(newState);
            tool.setAvailable(newState.equals("Disponible"));
        }

        if (newAmount != null) {
            if (newAmount < 0) {
                throw new IllegalArgumentException("Amount cannot be negative.");
            }
            tool.setAmount(newAmount);
        }

        return toolRepository.save(tool);
    }

    public ToolEntity getToolByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }
        return toolRepository.findByName(name.trim())
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tool not found by name: " + name));
    }

    public List<NameCategory> getAllNamesWithCategory() {
        List<ToolEntity> tools = toolRepository.findAll();

        // Usamos un LinkedHashMap para mantener orden de inserción y evitar duplicados
        Map<String, NameCategory> unique = new LinkedHashMap<>();
        for (ToolEntity t : tools) {
            if (t.getName() == null || t.getCategory() == null) continue;
            String key = t.getName() + "||" + t.getCategory();
            unique.putIfAbsent(key, new NameCategory(t.getName(), t.getCategory()));
        }
        return new ArrayList<>(unique.values());
    }
    public List<ToolEntity> listAvailable() {
        return toolRepository.findAllByInitialStateIgnoreCaseAndAmountGreaterThan("Disponible", 0);
    }

    // POJO simple para la respuesta
    @Data
    @AllArgsConstructor
    public static class NameCategory {
        private String name;
        private String category;
    }


}
