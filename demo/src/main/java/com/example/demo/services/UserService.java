package com.example.demo.services;

import com.example.demo.entities.UserEntity;
import com.example.demo.repositories.LoanRepository;
import com.example.demo.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    @Autowired private UserRepository userRepository;
    @Autowired private LoanRepository loanRepository;

    // PROVISIONING FROM JWT (reads RUT/phone number if they are in the token)
    public UserEntity provisionFromJwt(Jwt jwt) {
        String kcId  = jwt.getSubject(); // sub
        String email = jwt.getClaimAsString("email");
        String name  = jwt.getClaimAsString("name");
        if (name == null) {
            name = jwt.getClaimAsString("preferred_username");
        }

        boolean isAdmin;
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof List<?> roles) {
            isAdmin = roles.contains("admin") || roles.contains("ADMIN");
        } else {
            isAdmin = false;
        }

        //Optional claims that expose via mappers in Keycloak
        String rutClaim   = jwt.getClaimAsString("rut");
        String phoneClaim = jwt.getClaimAsString("phone");

        String finalName = name;
        String finalName1 = name;
        return userRepository.findByKeycloakId(kcId).map(u -> {
            u.setEmail(email);
            u.setName(finalName1);
            u.setAdmin(isAdmin);


            if (rutClaim != null && !rutClaim.isBlank() && u.getRut() == null) {
                String normalized = normalizeRut(rutClaim);
                // uniqueness: if it already exists for another user, error
                UserEntity other = userRepository.findByRut(normalized);
                if (other != null && !other.getId().equals(u.getId())) {
                    throw new IllegalArgumentException("RUT ya registrado por otro usuario");
                }
                u.setRut(normalized);
            }

            // Phone: If it comes, try to parse
            if (phoneClaim != null && !phoneClaim.isBlank()) {
                Integer phoneParsed = tryParsePhone(phoneClaim);
                if (phoneParsed != null) {
                    u.setPhone(phoneParsed);
                }
            }

            return userRepository.save(u);
        }).orElseGet(() -> {
            //Create
            //If it already existed via email and doesn't yet have a keycloakId, we'll adopt it.
            UserEntity existingByEmail = userRepository.findByEmail(email);
            if (existingByEmail != null && (existingByEmail.getKeycloakId() == null || existingByEmail.getKeycloakId().isBlank())) {
                existingByEmail.setKeycloakId(kcId);
                existingByEmail.setName(finalName);
                existingByEmail.setAdmin(isAdmin);

                // RUT from token (optional)
                if (rutClaim != null && !rutClaim.isBlank() && existingByEmail.getRut() == null) {
                    String normalized = normalizeRut(rutClaim);
                    UserEntity other = userRepository.findByRut(normalized);
                    if (other != null && !other.getId().equals(existingByEmail.getId())) {
                        throw new IllegalArgumentException("RUT ya registrado por otro usuario");
                    }
                    existingByEmail.setRut(normalized);
                }

                // phone from token (optional)
                if (phoneClaim != null && !phoneClaim.isBlank()) {
                    Integer phoneParsed = tryParsePhone(phoneClaim);
                    if (phoneParsed != null) {
                        existingByEmail.setPhone(phoneParsed);
                    }
                }
                return userRepository.save(existingByEmail);
            }

            // Create new
            UserEntity u = new UserEntity();
            u.setKeycloakId(kcId);
            u.setEmail(email);
            u.setName(finalName);
            u.setAdmin(isAdmin);
            u.setActive(true);
            u.setAmountOfLoans(0);

            // RUT did came in token
            if (rutClaim != null && !rutClaim.isBlank()) {
                String normalized = normalizeRut(rutClaim);
                // unicidad
                if (userRepository.findByRut(normalized) != null) {
                    throw new IllegalArgumentException("RUT ya registrado por otro usuario");
                }
                u.setRut(normalized);
            }

            // Phone did came in token
            if (phoneClaim != null && !phoneClaim.isBlank()) {
                Integer phoneParsed = tryParsePhone(phoneClaim);
                if (phoneParsed != null) {
                    u.setPhone(phoneParsed);
                }
            }

            return userRepository.save(u);
        });
    }

    // Manual Creation
    public UserEntity saveUser(UserEntity user) {
        UserEntity existingUserEmail = userRepository.findByEmail(user.getEmail());
        UserEntity existingUserRut = user.getRut() == null ? null : userRepository.findByRut(normalizeRut(user.getRut()));
        if (existingUserRut != null) {
            throw new IllegalArgumentException("User with this RUT already exists.");
        }
        if (existingUserEmail != null) {
            throw new IllegalArgumentException("User with this email already exists.");
        }

        String normalizedRut = user.getRut() == null ? null : normalizeRut(user.getRut());
        if (normalizedRut != null ) {
            throw new IllegalArgumentException("RUT inválido");
        }

        UserEntity newUser = new UserEntity(
                null,
                user.getKeycloakId(),
                user.getName(),
                user.getEmail(),
                null,
                normalizedRut,
                user.getPhone(),
                user.isAdmin(),
                true,
                0
        );
        return userRepository.save(newUser);
    }

    public UserEntity updateActive(UserEntity user, boolean active) {
        user.setActive(active);
        return userRepository.save(user);
    }

    public List<UserEntity> getAllUsers() { return userRepository.findAll(); }
    public UserEntity getUserById(Long id) { return userRepository.findById(id).orElse(null); }
    public UserEntity getUserByRut(String rut) { return userRepository.findByRut(rut == null ? null : normalizeRut(rut)); }


    public UserEntity recomputeActiveStatus(String rutUser) {
        String normalizedRut = rutUser == null ? null : normalizeRut(rutUser);
        UserEntity u = userRepository.findByRut(normalizedRut);
        if (u == null) return null;

        LocalDate today = LocalDate.now();

        boolean hasOverdue = loanRepository
                .existsByRutUserAndReturnDateBeforeAndLateReturnDateIsNull(normalizedRut, today);

        boolean hasUnpaidLateFine = loanRepository
                .existsByRutUserAndLateFineGreaterThanAndLateFinePaidIsFalse(normalizedRut, 0);

        boolean hasUnpaidDamage = loanRepository
                .existsByRutUserAndDamagePenaltyGreaterThanAndDamagePenaltyPaidIsFalse(normalizedRut, 0);

        boolean shouldBeActive = !(hasOverdue || hasUnpaidLateFine || hasUnpaidDamage);
        u.setActive(shouldBeActive);
        return userRepository.save(u);
    }

    // Helpers RUT/phone


    private String normalizeRut(String rut) {
        if (rut == null) return null;
        String raw = rut.replace(".", "").replace(" ", "").toUpperCase();
        if (!raw.contains("-")) {
            if (raw.length() < 2) return raw;
            raw = raw.substring(0, raw.length() - 1) + "-" + raw.substring(raw.length() - 1);
        }
        return raw;
    }



    private Integer tryParsePhone(String phone) {
        try {
            String digits = phone.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) return null;
            return Integer.parseInt(digits);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
