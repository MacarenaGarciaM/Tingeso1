package com.example.demo.controllers;

import com.example.demo.entities.ToolEntity;
import com.example.demo.entities.UserEntity;
import com.example.demo.services.ToolService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tool")
@CrossOrigin("*")
@RequiredArgsConstructor
public class ToolController {

    private final ToolService toolService;

    // Crear herramienta: body = ToolEntity, user via query param
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PostMapping
    public ResponseEntity<?> createTool(@RequestBody ToolEntity tool,
                                        @RequestParam String rutUser) {
        try {
            UserEntity user = new UserEntity();
            user.setRut(rutUser);
            return ResponseEntity.ok(toolService.saveTool(tool, user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Actualizar/mover/editar: todo vía query params
    @PreAuthorize("hasAnyRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTool(@PathVariable Long id,
                                        @RequestParam(required = false) String state,
                                        @RequestParam(required = false) Integer amount,
                                        @RequestParam(required = false) Integer repositionValue,
                                        @RequestParam String rutUser) {
        try {
            UserEntity user = new UserEntity();
            user.setRut(rutUser);
            return ResponseEntity.ok(toolService.updateTool(id, state, amount, repositionValue, user));
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
}
