// src/main/java/com/example/demo/services/UserService.java
package com.example.demo.services;

import com.example.demo.entities.UserEntity;
import com.example.demo.repositories.LoanRepository;
import com.example.demo.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoanRepository loanRepository;

    // ======= EXISTENTES =======

    public UserEntity saveUser(UserEntity user) {
        UserEntity existingUserEmail = userRepository.findByEmail(user.getEmail());
        UserEntity existingUserRut = userRepository.findByRut(user.getRut());
        if (existingUserRut != null) {
            throw new IllegalArgumentException("User with this RUT already exists.");
        }
        if (existingUserEmail != null) {
            throw new IllegalArgumentException("User with this email already exists.");
        }

        UserEntity newUser = new UserEntity(
                null,
                user.getName(),
                user.getEmail(),
                user.getPassword(),
                user.getRut(),
                user.getPhone(),
                user.isAdmin(),
                true,   // activo al crear
                0      // préstamos activos = 0
        );
        return userRepository.save(newUser);
    }

    public UserEntity updateActive(UserEntity user, boolean active) {
        user.setActive(active);
        return userRepository.save(user);
    }

    public List<UserEntity> getAllUsers() { return userRepository.findAll(); }
    public UserEntity getUserById(Long id) { return userRepository.findById(id).orElse(null); }
    public UserEntity getUserByRut(String rut) { return userRepository.findByRut(rut); }

    // ======= NUEVO: recalcular 'active' según reglas =======

    /**
     * Activo = true solo si NO tiene préstamos vencidos (returnDate < hoy y sin devolver)
     * y NO tiene multas/penalizaciones impagas.
     */
    public UserEntity recomputeActiveStatus(String rutUser) {
        UserEntity u = userRepository.findByRut(rutUser);
        if (u == null) return null;

        LocalDate today = LocalDate.now();

        boolean hasOverdue = loanRepository
                .existsByRutUserAndReturnDateBeforeAndLateReturnDateIsNull(rutUser, today);

        boolean hasUnpaidLateFine = loanRepository
                .existsByRutUserAndLateFineGreaterThanAndLateFinePaidIsFalse(rutUser, 0);

        boolean hasUnpaidDamage = loanRepository
                .existsByRutUserAndDamagePenaltyGreaterThanAndDamagePenaltyPaidIsFalse(rutUser, 0);

        boolean shouldBeActive = !(hasOverdue || hasUnpaidLateFine || hasUnpaidDamage);
        u.setActive(shouldBeActive);
        return userRepository.save(u);
    }
}
