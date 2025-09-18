package com.example.demo.controllers;

import com.example.demo.entities.ToolEntity;
import com.example.demo.entities.UserEntity;
import com.example.demo.services.ToolService;
import lombok.Getter;
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
    public ResponseEntity<?> createTool(@RequestBody ToolRequest request) {
        try {
            ToolEntity savedTool = toolService.saveTool(request.getTool(), request.getUser());
            return ResponseEntity.ok(savedTool);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Actualizar herramienta existente
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTool(@PathVariable Long id,
                                        @RequestParam(required = false) String state,
                                        @RequestParam(required = false) Integer amount,
                                        @RequestBody UserEntity user) {
        try {
            ToolEntity updatedTool = toolService.updateTool(id, state, amount, user);
            return ResponseEntity.ok(updatedTool);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @Getter
    public static class ToolRequest {
        private ToolEntity tool;
        private UserEntity user;

    }
}
