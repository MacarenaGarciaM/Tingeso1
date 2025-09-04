package com.example.demo.controllers;

import com.example.demo.entities.ToolEntity;
import com.example.demo.services.ToolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tool")
@CrossOrigin("*")
public class ToolController {

    @Autowired
    private ToolService toolService;

    // Crear nueva herramienta
    @PostMapping
    public ResponseEntity<?> createTool(@RequestBody ToolEntity tool) {
        try {
            ToolEntity savedTool = toolService.saveTool(tool);
            return ResponseEntity.ok(savedTool);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Actualizar herramienta existente
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTool(@PathVariable Long id,
                                        @RequestParam(required = false) String state,
                                        @RequestParam(required = false) Integer amount) {
        try {
            ToolEntity updatedTool = toolService.updateTool(id, state, amount);
            return ResponseEntity.ok(updatedTool);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
