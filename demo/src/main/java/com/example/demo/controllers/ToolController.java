package com.example.demo.controllers;

import com.example.demo.entities.ToolEntity;
import com.example.demo.entities.UserEntity;
import com.example.demo.services.ToolService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tool")
@CrossOrigin("*")
public class ToolController {

    @Autowired
    private ToolService toolService;

    // Crear nueva herramienta
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PostMapping
    public ResponseEntity<?> createTool(@RequestBody ToolRequest request) {
        try {
            ToolEntity savedTool = toolService.saveTool(request.getTool(), request.getUser());
            return ResponseEntity.ok(savedTool);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTool(@PathVariable Long id,
                                        @RequestParam(required = false) String state,
                                        @RequestParam(required = false) Integer amount,
                                        @RequestParam(required = false) Integer repositionValue,
                                        @RequestBody UserEntity user) {
        try {
            ToolEntity updatedTool = toolService.updateTool(id, state, amount, repositionValue, user);
            return ResponseEntity.ok(updatedTool);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/names-categories")
    public ResponseEntity<List<ToolService.NameCategory>> listNamesWithCategory() {
        return ResponseEntity.ok(toolService.getAllNamesWithCategory());
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/available")
    public ResponseEntity<List<ToolEntity>> listAvailable() {
        return ResponseEntity.ok(toolService.listAvailable());
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @GetMapping("/by-state")
    public ResponseEntity<List<ToolEntity>> listByState(@RequestParam String state) {
        return ResponseEntity.ok(toolService.listByState(state));
    }


    @Getter
    public static class ToolRequest {
        private ToolEntity tool;
        private UserEntity user;

    }
}
