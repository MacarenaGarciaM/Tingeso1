package com.example.demo.services;

import com.example.demo.entities.ToolEntity;
import com.example.demo.repositories.ToolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class ToolService {
    @Autowired
    ToolRepository toolRepository;

    private static final List<String> ValidStates = Arrays.asList("Disponible", "Prestada", "En reparación", "Dada de baja");
    public ToolEntity saveTool(ToolEntity tool) {
        if (tool.getName() == null || tool.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tool name is required.");
        }
        if (tool.getCategory() == null || tool.getCategory().trim().isEmpty()) {
            throw new IllegalArgumentException("Tool category is required.");
        }
        if (tool.getRepositionValue() <= 0) {
            throw new IllegalArgumentException("Reposition value must be greater than 0.");
        }
        if (tool.getInitialState() == null || tool.getInitialState().isEmpty()){
            throw new IllegalArgumentException("Initial state is required.");

        }
        if (!ValidStates.contains(tool.getInitialState())){
            throw new IllegalArgumentException("Initial state is not valid.");
        }

        ToolEntity newtool = new ToolEntity (null,tool.getName(), tool.getCategory(), tool.getInitialState(),  tool.getRepositionValue(), tool.isAvailable(), tool.getAmount());
        return toolRepository.save(newtool);
    }

}

