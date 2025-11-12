package com.example.demo.repositories;

import com.example.demo.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    UserEntity findByRut(String rut);
    UserEntity findByEmail(String email);
    Optional<UserEntity> findByKeycloakId(String keycloakId);

}
