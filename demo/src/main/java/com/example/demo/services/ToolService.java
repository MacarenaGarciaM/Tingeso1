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
        // Basic validations
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

        // Check if a tool with the same name and category already exists
        List<ToolEntity> existing = toolRepository
                .findByNameAndCategoryAndInitialState(tool.getName(), tool.getCategory(), tool.getInitialState());

        ToolEntity savedTool;
        if (!existing.isEmpty()) {
            ToolEntity bucket = existing.get(0);
            bucket.setAmount(bucket.getAmount() + tool.getAmount());
            bucket.setAvailable("Disponible".equalsIgnoreCase(bucket.getInitialState()));
            bucket.setRepositionValue(tool.getRepositionValue()); // si quieres actualizarlo
            savedTool = toolRepository.save(bucket);
        } else {
            ToolEntity newTool = new ToolEntity(
                    null,
                    tool.getName(),
                    tool.getCategory(),
                    tool.getInitialState(),
                    tool.getRepositionValue(),
                    "Disponible".equalsIgnoreCase(tool.getInitialState()),
                    tool.getAmount()
            );
            savedTool = toolRepository.save(newTool);
        }

        // Record movement in kardex (only the amount entered, not the total)
        KardexEntity kardex = new KardexEntity();
        kardex.setTool(savedTool);
        kardex.setRutUser(rutUser.getRut()); // Usuario autenticado aquí
        kardex.setType("Ingreso");
        kardex.setMovementDate(LocalDate.now());
        kardex.setStock(tool.getAmount()); // SOLO la cantidad ingresada
        kardexRepository.save(kardex);

        return savedTool;
    }

    public ToolEntity updateTool(Long id, String newState, Integer newAmount,
                                 Integer newRepositionValue, UserEntity rutUser) {

        ToolEntity tool = toolRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found"));

        if (newState != null) {
            if (!validState.contains(newState)) {
                throw new IllegalArgumentException("Invalid state: " + newState);
            }

            // Case 1: Disponible -> another state
            if (tool.getInitialState().equals("Disponible") && !newState.equals("Disponible")) {
                if (tool.getAmount() <= 0) {
                    throw new IllegalArgumentException("No hay stock disponible para mover a otro estado.");
                }

                // Reduce in Disponible
                tool.setAmount(tool.getAmount() - 1);
                toolRepository.save(tool);

                // Check if a tool with the same status already exists.
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

            // Case 2: another state -> Disponible
            if (!tool.getInitialState().equals("Disponible") && newState.equals("Disponible")) {
                if (tool.getAmount() <= 0) {
                    throw new IllegalArgumentException("No hay stock en este estado para devolver a Disponible.");
                }

                // Reduce actual state
                tool.setAmount(tool.getAmount() - 1);
                toolRepository.save(tool);

                // search Disponible tool
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

            // Case 3: state A (≠ Disponible) → state B (≠ Disponible)
            if (!tool.getInitialState().equals("Disponible") && !newState.equals("Disponible")) {

                if (tool.getAmount() <= 0) {
                    throw new IllegalArgumentException("No hay stock en este estado para mover.");
                }

                // 1) Sustract from origin bucket
                tool.setAmount(tool.getAmount() - 1);
                toolRepository.save(tool);

                // 2) Search/create destination bucket (same name+category+reposition, state = newState)
                Optional<ToolEntity> optTarget =
                        toolRepository.findFirstByNameAndCategoryAndInitialState(
                                tool.getName(), tool.getCategory(), newState);

                ToolEntity target = optTarget.orElseGet(() -> new ToolEntity(
                        null,
                        tool.getName(),
                        tool.getCategory(),
                        newState,
                        tool.getRepositionValue(),
                        false,      // available only in "Disponible"
                        0
                ));
                target.setAmount(target.getAmount() + 1);
                ToolEntity savedTarget = toolRepository.save(target);

                // 3) Kardex
                KardexEntity k = new KardexEntity();
                k.setTool(savedTarget);
                k.setRutUser(rutUser.getRut());
                k.setType("Cambio de estado: " + newState);
                k.setMovementDate(LocalDate.now());
                k.setStock(savedTarget.getAmount());
                kardexRepository.save(k);

                return savedTarget;
            }
        }
        if (newAmount != null) {
            if (newAmount < 0) throw new IllegalArgumentException("Amount cannot be negative.");
            tool.setAmount(newAmount);
        }

        if (newRepositionValue != null) {
            if (newRepositionValue < 0) throw new IllegalArgumentException("Reposition value cannot be negative.");
            tool.setRepositionValue(newRepositionValue);
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

        // Uses a LinkedHashMap to maintain insertion order and avoid duplicates
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

    public List<ToolEntity> listByState(String state) {
        if (state == null || state.isBlank()) throw new IllegalArgumentException("state is required");
        if ("Disponible".equalsIgnoreCase(state)) {
            return toolRepository.findAllByInitialStateIgnoreCaseAndAmountGreaterThan(state, 0);
        }
        return toolRepository.findAllByInitialStateIgnoreCase(state);
    }

    @Data
    @AllArgsConstructor
    public static class NameCategory {
        private String name;
        private String category;
    }


}
