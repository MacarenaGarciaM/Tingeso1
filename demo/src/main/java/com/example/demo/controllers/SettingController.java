// src/main/java/com/example/demo/controllers/SettingsController.java
package com.example.demo.controllers;

import com.example.demo.services.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/settings")
@CrossOrigin("*")
@RequiredArgsConstructor
public class SettingController {

    private final SettingService setting;

    // visible para usuarios logueados (o cámbialo a permitAll si quieres público)
    @GetMapping("/daily-rate")
    public ResponseEntity<Map<String, Object>> getDailyRate() {
        return ResponseEntity.ok(Map.of("value", setting.getDailyRentPrice()));
    }

    // sólo admin puede actualizar
    @PreAuthorize("hasAnyRole('ADMIN')")
    @PutMapping("/daily-rate")
    public ResponseEntity<Map<String, Object>> updateDailyRate(@RequestBody Map<String, Object> body) {
        Object raw = body.get("value");
        if (raw == null) return ResponseEntity.badRequest().body(Map.of("error","value is required"));
        int v = (raw instanceof Number n) ? n.intValue() : Integer.parseInt(raw.toString());
        int saved = setting.setDailyRentPrice(v);
        return ResponseEntity.ok(Map.of("value", saved));
    }
}
