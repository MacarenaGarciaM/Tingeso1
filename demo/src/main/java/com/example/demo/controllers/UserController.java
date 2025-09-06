package com.example.demo.controllers;

import com.example.demo.entities.UserEntity;
import com.example.demo.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * Crear un nuevo usuario
     */
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody UserEntity user) {
        try {
            UserEntity savedUser = userService.saveUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Obtener todos los usuarios
     */
    @GetMapping
    public ResponseEntity<List<UserEntity>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * Obtener usuario por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        UserEntity user = userService.getUserById(id);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
        return ResponseEntity.ok(user);
    }

    /**
     * Obtener usuario por RUT
     */
    @GetMapping("/rut/{rut}")
    public ResponseEntity<?> getUserByRut(@PathVariable String rut) {
        UserEntity user = userService.getUserByRut(rut);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
        return ResponseEntity.ok(user);
    }

    /**
     * Actualizar datos de usuario (nombre, email, teléfono, etc.)

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UserEntity updatedUser) {
        try {
            UserEntity user = userService.updateUser(id, updatedUser);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }*/

    /**
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long id) {
        try {
            UserEntity user = userService.toggleUserStatus(id);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    } */

    /**
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        boolean deleted = userService.deleteUser(id);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
        return ResponseEntity.ok("User deleted successfully");
    }*/
}
