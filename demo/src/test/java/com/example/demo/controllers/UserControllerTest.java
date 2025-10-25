package com.example.demo.controllers;

import com.example.demo.entities.UserEntity;
import com.example.demo.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(UserControllerTest.MethodSecurityCfg.class)
class UserControllerTest {

    @TestConfiguration
    @EnableMethodSecurity // evalúa @PreAuthorize del controller
    static class MethodSecurityCfg {}

    @Autowired MockMvc mvc;

    @MockitoBean UserService userService;

    // ───────────── POST /users (USER/ADMIN) ─────────────
    @Test
    void createUser_created_userRole() throws Exception {
        UserEntity saved = new UserEntity();
        saved.setId(10L);
        saved.setRut("11.111.111-1");
        saved.setName("Ana");
        saved.setActive(true);

        given(userService.saveUser(org.mockito.ArgumentMatchers.any(UserEntity.class))).willReturn(saved);

        String body = """
        {"rut":"11.111.111-1","name":"Ana","active":true}
        """;

        mvc.perform(post("/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.name", is("Ana")));
    }

    @Test
    void createUser_badRequest_whenServiceThrows() throws Exception {
        given(userService.saveUser(org.mockito.ArgumentMatchers.any(UserEntity.class)))
                .willThrow(new IllegalArgumentException("RUT already exists"));

        mvc.perform(post("/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rut\":\"11.111.111-1\",\"name\":\"Ana\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("RUT already exists")));
    }

    @Test
    void createUser_unauthenticated_returns401() throws Exception {
        mvc.perform(post("/users")
                        .with(csrf()) // si no, sería 403 por CSRF
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ───────────── GET /users (USER/ADMIN) ─────────────
    @Test
    void getAllUsers_ok_userRole() throws Exception {
        UserEntity u1 = new UserEntity(); u1.setId(1L); u1.setName("Ana");
        UserEntity u2 = new UserEntity(); u2.setId(2L); u2.setName("Bruno");

        given(userService.getAllUsers()).willReturn(List.of(u1, u2));

        mvc.perform(get("/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Ana")))
                .andExpect(jsonPath("$[1].name", is("Bruno")));
    }

    @Test
    void getAllUsers_unauthenticated_returns401() throws Exception {
        mvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());
    }

    // ───────────── GET /users/{id} (ADMIN) ─────────────
    @Test
    void getUserById_ok_adminRole() throws Exception {
        UserEntity u = new UserEntity(); u.setId(5L); u.setName("Carlos");
        given(userService.getUserById(5L)).willReturn(u);

        mvc.perform(get("/users/{id}", 5L)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(5)))
                .andExpect(jsonPath("$.name", is("Carlos")));
    }

    @Test
    void getUserById_notFound_adminRole() throws Exception {
        given(userService.getUserById(99L)).willReturn(null);

        mvc.perform(get("/users/{id}", 99L)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("User not found")));
    }

    @Test
    void getUserById_forbidden_whenNotAdmin() throws Exception {
        mvc.perform(get("/users/{id}", 1L)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserById_unauthenticated_returns401() throws Exception {
        mvc.perform(get("/users/{id}", 1L))
                .andExpect(status().isUnauthorized());
    }

    // ───────────── GET /users/rut/{rut} (ADMIN) ─────────────
    @Test
    void getUserByRut_ok_adminRole() throws Exception {
        UserEntity u = new UserEntity(); u.setId(3L); u.setRut("22.222.222-2"); u.setName("Diana");
        given(userService.getUserByRut("22.222.222-2")).willReturn(u);

        mvc.perform(get("/users/rut/{rut}", "22.222.222-2")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Diana")));
    }

    @Test
    void getUserByRut_notFound_adminRole() throws Exception {
        given(userService.getUserByRut("00.000.000-0")).willReturn(null);

        mvc.perform(get("/users/rut/{rut}", "00.000.000-0")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("User not found")));
    }

    @Test
    void getUserByRut_forbidden_whenNotAdmin() throws Exception {
        mvc.perform(get("/users/rut/{rut}", "11.111.111-1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserByRut_unauthenticated_returns401() throws Exception {
        mvc.perform(get("/users/rut/{rut}", "11.111.111-1"))
                .andExpect(status().isUnauthorized());
    }

    // ───────────── PATCH /users/{id}/active (ADMIN) ─────────────
    @Test
    void updateUserActiveStatus_ok_adminRole() throws Exception {
        UserEntity u = new UserEntity(); u.setId(7L); u.setActive(true);
        given(userService.getUserById(7L)).willReturn(u);

        UserEntity updated = new UserEntity(); updated.setId(7L); updated.setActive(false);
        given(userService.updateActive(u, false)).willReturn(updated);

        mvc.perform(patch("/users/{id}/active", 7L)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .with(csrf())
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(7)))
                .andExpect(jsonPath("$.active", is(false)));
    }

    @Test
    void updateUserActiveStatus_notFound_adminRole() throws Exception {
        given(userService.getUserById(77L)).willReturn(null);

        mvc.perform(patch("/users/{id}/active", 77L)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .with(csrf())
                        .param("active", "true"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("User not found")));
    }

    @Test
    void updateUserActiveStatus_errorFromService_returns500() throws Exception {
        UserEntity u = new UserEntity(); u.setId(8L); u.setActive(true);
        given(userService.getUserById(8L)).willReturn(u);
        // cualquier excepción chequeada o runtime en updateActive -> 500
        doThrow(new RuntimeException("db down")).when(userService).updateActive(u, true);

        mvc.perform(patch("/users/{id}/active", 8L)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .with(csrf())
                        .param("active", "true"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Error updating user status")));
    }

    @Test
    void updateUserActiveStatus_forbidden_whenNotAdmin() throws Exception {
        mvc.perform(patch("/users/{id}/active", 1L)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .with(csrf())
                        .param("active", "true"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUserActiveStatus_unauthenticated_returns401() throws Exception {
        mvc.perform(patch("/users/{id}/active", 1L)
                        .with(csrf())
                        .param("active", "true"))
                .andExpect(status().isUnauthorized());
    }
}
