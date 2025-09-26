package com.example.demo.services;

import com.example.demo.entities.UserEntity;
import com.example.demo.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository UserRepository;

    public UserEntity saveUser(UserEntity user) {

        UserEntity existingUserEmail = UserRepository.findByEmail(user.getEmail());
        UserEntity existingUserRut = UserRepository.findByRut(user.getRut());
        if (existingUserRut != null) {
            throw new IllegalArgumentException("User with this RUT already exists.");
        }

        if (existingUserEmail != null) {
            throw new IllegalArgumentException("User with this email already exists.");
        }

        // Crear usuario nuevo con valores iniciales definidos por la épica 3
        UserEntity newUser = new UserEntity(
                null,                                 // id autogenerado
                user.getName(),
                user.getEmail(),
                user.getPassword(),
                user.getRut(),
                user.getPhone(),
                user.isAdmin(),                        // rol (true=admin, false=empleado)
                true,                                // status inicial = Activo
                0                                    // cantidad de préstamos inicial = 0
        );

        return UserRepository.save(newUser);
    }

    public UserEntity updateActive(UserEntity user, boolean active) {
        user.setActive(active); // Usar el parámetro recibido
        return UserRepository.save(user);
    }

    public List<UserEntity> getAllUsers() {
        return UserRepository.findAll(); // Debe usar el método del repositorio
    }

    public UserEntity getUserById(Long id) {
        return UserRepository.findById(id).orElse(null);
    }

    public UserEntity getUserByRut(String rut) {
        return UserRepository.findByRut(rut);
    }


}
