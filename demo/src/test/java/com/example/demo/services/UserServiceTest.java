package com.example.demo.services;

import com.example.demo.entities.UserEntity;
import com.example.demo.repositories.LoanRepository;
import com.example.demo.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock LoanRepository loanRepository;

    @InjectMocks UserService userService;

    private Jwt jwtBase;

    @BeforeEach
    void setUp() {
        jwtBase = Jwt.withTokenValue("t")
                .header("alg","none")
                .subject("kc-123")
                .claim("email", "ana@example.com")
                .claim("name",  "Ana")
                .claim("realm_access", Map.of("roles", List.of("user")))
                .build();
    }

    // ───────────────── provisionFromJwt: UPDATE por keycloakId ─────────────────

    @Test
    void provisionFromJwt_updatesExistingByKeycloakId_setsAdminRutPhone_andSaves() {
        UserEntity existing = new UserEntity();
        existing.setId(10L);
        existing.setKeycloakId("kc-123");
        existing.setAdmin(false);
        existing.setRut(null);

        Jwt jwt = Jwt.withTokenValue("t").header("alg","none")
                .subject("kc-123")
                .claim("email", "ana@example.com")
                .claim("preferred_username", "AnaKC") // se usa tal cual
                .claim("realm_access", Map.of("roles", List.of("user","admin")))
                .claim("rut", "11.111.111-1") // se normaliza -> "11111111-1"
                .claim("phone", "+56 9 1234 5678") // desborda -> queda 0
                .build();

        given(userRepository.findByKeycloakId("kc-123")).willReturn(Optional.of(existing));
        given(userRepository.findByRut("11111111-1")).willReturn(null); // unicidad OK
        given(userRepository.save(any(UserEntity.class))).willAnswer(inv -> inv.getArgument(0));

        UserEntity out = userService.provisionFromJwt(jwt);

        assertEquals("ana@example.com", out.getEmail());
        assertEquals("AnaKC", out.getName()); // preferred_username sin upper
        assertTrue(out.isAdmin());
        assertEquals("11111111-1", out.getRut());
        assertEquals(0, out.getPhone()); // int → desborde => 0
        verify(userRepository).save(out);
    }

    // ─────────────── provisionFromJwt: adopta por email si no hay KC ───────────────

    @Test
    void provisionFromJwt_adoptsExistingByEmail_whenNoKeycloakIdYet() {
        UserEntity byEmail = new UserEntity();
        byEmail.setId(5L);
        byEmail.setEmail("ana@example.com");
        byEmail.setKeycloakId(null);
        byEmail.setRut(null);

        Jwt jwt = Jwt.withTokenValue("t").header("alg","none")
                .subject("kc-999")
                .claim("email", "ana@example.com")
                .claim("name", "Ana")
                .claim("realm_access", Map.of("roles", List.of("ADMIN"))) // mayúsculas válidas
                .claim("rut", "11.111.111-1") // -> "11111111-1"
                .claim("phone", "(+56) 9 3333-2222") // desborda -> 0
                .build();

        given(userRepository.findByKeycloakId("kc-999")).willReturn(Optional.empty());
        given(userRepository.findByEmail("ana@example.com")).willReturn(byEmail);
        given(userRepository.findByRut("11111111-1")).willReturn(null);
        given(userRepository.save(any(UserEntity.class))).willAnswer(inv -> inv.getArgument(0));

        UserEntity out = userService.provisionFromJwt(jwt);

        assertEquals("kc-999", out.getKeycloakId());
        assertEquals("Ana", out.getName());
        assertTrue(out.isAdmin());
        assertEquals("11111111-1", out.getRut());
        assertEquals(0, out.getPhone());
        verify(userRepository).save(out);
    }

    // ───────────────── provisionFromJwt: crea nuevo ─────────────────

    @Test
    void provisionFromJwt_createsNew_whenNoKcAndNoEmailMatch() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg","none")
                .subject("kc-new")
                .claim("email", "new@example.com")
                .claim("name", "Nuevo")
                .claim("realm_access", Map.of("roles", List.of("user")))
                .claim("rut", "12.345.678-k") // -> "12345678-K"
                .build();

        given(userRepository.findByKeycloakId("kc-new")).willReturn(Optional.empty());
        given(userRepository.findByEmail("new@example.com")).willReturn(null);
        given(userRepository.findByRut("12345678-K")).willReturn(null);
        given(userRepository.save(any(UserEntity.class))).willAnswer(inv -> {
            UserEntity u = inv.getArgument(0);
            u.setId(100L);
            return u;
        });

        UserEntity out = userService.provisionFromJwt(jwt);

        assertEquals(100L, out.getId());
        assertEquals("kc-new", out.getKeycloakId());
        assertEquals("new@example.com", out.getEmail());
        assertEquals("Nuevo", out.getName());
        assertTrue(out.isActive());
        assertEquals(0, out.getAmountOfLoans());
    }

    @Test
    void provisionFromJwt_create_conflictsOnRut_throws() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg","none")
                .subject("kc-new")
                .claim("email", "new@example.com")
                .claim("name", "Nuevo")
                .claim("rut", "11111111-1") // ya normalizado
                .build();

        given(userRepository.findByKeycloakId("kc-new")).willReturn(Optional.empty());
        given(userRepository.findByEmail("new@example.com")).willReturn(null);
        given(userRepository.findByRut("11111111-1")).willReturn(new UserEntity());

        assertThrows(IllegalArgumentException.class, () -> userService.provisionFromJwt(jwt));
        verify(userRepository, never()).save(any());
    }

    // ───────────────── saveUser ─────────────────

    @Test
    void saveUser_fails_whenEmailExists() {
        UserEntity input = new UserEntity();
        input.setEmail("dup@example.com");
        given(userRepository.findByEmail("dup@example.com")).willReturn(new UserEntity());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.saveUser(input));
        assertTrue(ex.getMessage().toLowerCase().contains("email"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void saveUser_fails_whenRutExists() {
        UserEntity input = new UserEntity();
        input.setEmail("ok@example.com");
        input.setRut("11.111.111-1"); // -> "11111111-1"

        given(userRepository.findByEmail("ok@example.com")).willReturn(null);
        given(userRepository.findByRut("11111111-1")).willReturn(new UserEntity());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.saveUser(input));
        // mensaje exacto del service: "User with this RUT already exists."
        assertTrue(ex.getMessage().toLowerCase().contains("rut already exists"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void saveUser_fails_withRutInvalid_perCurrentLogic() {
        UserEntity input = new UserEntity();
        input.setEmail("ok2@example.com");
        input.setRut("11.111.111-1");

        given(userRepository.findByEmail("ok2@example.com")).willReturn(null);
        // no importa el findByRut porque falla antes por tu lógica actual
        given(userRepository.findByRut(anyString())).willReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.saveUser(input));
        assertTrue(ex.getMessage().toLowerCase().contains("inválido"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void saveUser_ok_withoutRut() {
        UserEntity input = new UserEntity();
        input.setEmail("ok@example.com");
        input.setName("Ana");

        given(userRepository.findByEmail("ok@example.com")).willReturn(null);
        given(userRepository.save(any(UserEntity.class))).willAnswer(inv -> {
            UserEntity u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserEntity out = userService.saveUser(input);

        assertEquals(1L, out.getId());
        assertEquals("ok@example.com", out.getEmail());
        assertEquals("Ana", out.getName());
        assertTrue(out.isActive());
        assertEquals(0, out.getAmountOfLoans());
    }

    // ───────────────── updateActive / getters ─────────────────

    @Test
    void updateActive_setsAndPersists() {
        UserEntity u = new UserEntity(); u.setActive(true);
        given(userRepository.save(u)).willReturn(u);

        UserEntity out = userService.updateActive(u, false);
        assertFalse(out.isActive());
        verify(userRepository).save(u);
    }

    @Test
    void basicDelegates_getAllUsers_getById_getByRut() {
        given(userRepository.findAll()).willReturn(List.of(new UserEntity()));
        assertEquals(1, userService.getAllUsers().size());

        given(userRepository.findById(7L)).willReturn(Optional.of(new UserEntity()));
        assertNotNull(userService.getUserById(7L));

        // entrada "11.111.111-1" se normaliza -> "11111111-1"
        given(userRepository.findByRut("11111111-1")).willReturn(new UserEntity());
        assertNotNull(userService.getUserByRut("11.111.111-1"));
    }

    // ───────────────── recomputeActiveStatus ─────────────────

    @Test
    void recomputeActiveStatus_setsInactive_whenAnyDebtOrOverdue() {
        UserEntity u = new UserEntity(); u.setRut("11111111-1"); u.setActive(true);
        // la normalización convierte "11.111.111-1" -> "11111111-1"
        given(userRepository.findByRut("11111111-1")).willReturn(u);

        given(loanRepository.existsByRutUserAndReturnDateBeforeAndLateReturnDateIsNull(eq("11111111-1"), any()))
                .willReturn(true);
        given(loanRepository.existsByRutUserAndLateFineGreaterThanAndLateFinePaidIsFalse("11111111-1", 0))
                .willReturn(false);
        given(loanRepository.existsByRutUserAndDamagePenaltyGreaterThanAndDamagePenaltyPaidIsFalse("11111111-1", 0))
                .willReturn(false);

        given(userRepository.save(any(UserEntity.class))).willAnswer(inv -> inv.getArgument(0));

        UserEntity out = userService.recomputeActiveStatus("11.111.111-1");
        assertNotNull(out);
        assertFalse(out.isActive());
        verify(userRepository).save(out);
    }

    @Test
    void recomputeActiveStatus_setsActive_whenNoIssues() {
        UserEntity u = new UserEntity(); u.setRut("22222222-2"); u.setActive(false);
        given(userRepository.findByRut("22222222-2")).willReturn(u);

        given(loanRepository.existsByRutUserAndReturnDateBeforeAndLateReturnDateIsNull(eq("22222222-2"), any()))
                .willReturn(false);
        given(loanRepository.existsByRutUserAndLateFineGreaterThanAndLateFinePaidIsFalse("22222222-2", 0))
                .willReturn(false);
        given(loanRepository.existsByRutUserAndDamagePenaltyGreaterThanAndDamagePenaltyPaidIsFalse("22222222-2", 0))
                .willReturn(false);

        given(userRepository.save(any(UserEntity.class))).willAnswer(inv -> inv.getArgument(0));

        UserEntity out = userService.recomputeActiveStatus("22.222.222-2");
        assertTrue(out.isActive());
    }

    @Test
    void recomputeActiveStatus_returnsNull_whenUserNotFound() {
        // "00.000.000-0" -> "00000000-0"
        given(userRepository.findByRut("00000000-0")).willReturn(null);
        assertNull(userService.recomputeActiveStatus("00.000.000-0"));
        verify(userRepository, never()).save(any());
    }
}
