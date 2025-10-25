package com.example.demo.controllers;

import com.example.demo.entities.ToolEntity;
import com.example.demo.entities.UserEntity;
import com.example.demo.services.ToolService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
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

@WebMvcTest(ToolController.class)
@Import(ToolControllerTest.MethodSecurityCfg.class)
class ToolControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityCfg {
    }

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ToolService toolService;

    //Controller real Bean
    @Autowired
    ToolController controller;

    //MockMvc
    @Test
    void createTool_ok_userRole() throws Exception {
        ToolEntity saved = new ToolEntity();
        saved.setId(100L);
        saved.setName("Taladro");
        saved.setCategory("Eléctricas");
        saved.setInitialState("GOOD");
        saved.setRepositionValue(50000);
        saved.setAvailable(true);
        saved.setAmount(5);

        given(toolService.saveTool(org.mockito.ArgumentMatchers.any(ToolEntity.class),
                argThat(u -> u != null && "11.111.111-1".equals(u.getRut()))))
                .willReturn(saved);

        String toolJson = """
                {
                  "name": "Taladro",
                  "category": "Eléctricas",
                  "initialState": "GOOD",
                  "repositionValue": 50000,
                  "available": true,
                  "amount": 5
                }
                """;

        mvc.perform(post("/tool")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .param("rutUser", "11.111.111-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toolJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(100)))
                .andExpect(jsonPath("$.name", is("Taladro")))
                .andExpect(jsonPath("$.amount", is(5)))
                .andExpect(jsonPath("$.available", is(true)));
    }

    @Test
    void createTool_badRequest_whenServiceThrowsIAE() throws Exception {
        given(toolService.saveTool(org.mockito.ArgumentMatchers.any(ToolEntity.class), org.mockito.ArgumentMatchers.any(UserEntity.class)))
                .willThrow(new IllegalArgumentException("Tool already exists"));

        String toolJson = """
                {"name":"Taladro","category":"Eléctricas","initialState":"GOOD","repositionValue":50000,"available":true,"amount":5}
                """;

        mvc.perform(post("/tool")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .param("rutUser", "11.111.111-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toolJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Tool already exists")));
    }

    @Test
    void createTool_unauthenticated_returns401() throws Exception {
        mvc.perform(post("/tool")
                        .with(csrf()) // sin CSRF te daría 403
                        .param("rutUser", "11.111.111-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateTool_unauthenticated_returns401() throws Exception {
        mvc.perform(put("/tool/{id}", 9L)
                        .with(csrf())
                        .param("rutUser", "11.111.111-1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateTool_ok_adminRole_allParams() throws Exception {
        ToolEntity updated = new ToolEntity();
        updated.setId(5L);
        updated.setName("Sierra");
        updated.setCategory("Manuales");
        updated.setInitialState("DAMAGED");
        updated.setRepositionValue(20000);
        updated.setAvailable(true);
        updated.setAmount(7);

        given(toolService.updateTool(eq(5L), eq("DAMAGED"), eq(7), eq(20000),
                argThat(u -> u != null && "11.111.111-1".equals(u.getRut()))))
                .willReturn(updated);

        mvc.perform(put("/tool/{id}", 5L)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .param("state", "DAMAGED")
                        .param("amount", "7")
                        .param("repositionValue", "20000")
                        .param("rutUser", "11.111.111-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(5)))
                .andExpect(jsonPath("$.initialState", is("DAMAGED")))
                .andExpect(jsonPath("$.amount", is(7)))
                .andExpect(jsonPath("$.repositionValue", is(20000)));
    }

    @Test
    void updateTool_badRequest_whenServiceThrowsIAE() throws Exception {
        doThrow(new IllegalArgumentException("Invalid state"))
                .when(toolService).updateTool(eq(8L), eq("XXX"), eq(1), eq(null),
                        argThat(u -> "22.222.222-2".equals(u.getRut())));

        mvc.perform(put("/tool/{id}", 8L)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .param("state", "XXX")
                        .param("amount", "1")
                        .param("rutUser", "22.222.222-2"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid state")));
    }

    @Test
    void updateTool_forbidden_whenNotAdmin() throws Exception {
        mvc.perform(put("/tool/{id}", 5L)
                        .with(jwt()) // autenticado sin ROLE_ADMIN
                        .param("state", "GOOD")
                        .param("rutUser", "11.111.111-1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listNamesWithCategory_ok_userRole() throws Exception {
        given(toolService.getAllNamesWithCategory()).willReturn(List.of());

        mvc.perform(get("/tool/names-categories")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listAvailable_ok_userRole() throws Exception {
        ToolEntity t = new ToolEntity();
        t.setId(1L);
        t.setName("Martillo");
        t.setCategory("Manuales");
        t.setInitialState("GOOD");
        t.setRepositionValue(10000);
        t.setAvailable(true);
        t.setAmount(3);

        given(toolService.listAvailable()).willReturn(List.of(t));

        mvc.perform(get("/tool/available")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Martillo")))
                .andExpect(jsonPath("$[0].available", is(true)));
    }

    @Test
    void listByState_ok_adminRole() throws Exception {
        ToolEntity t = new ToolEntity();
        t.setId(2L);
        t.setName("Serrucho");
        t.setCategory("Manuales");
        t.setInitialState("DAMAGED");
        t.setRepositionValue(8000);
        t.setAvailable(false);
        t.setAmount(1);

        given(toolService.listByState("DAMAGED")).willReturn(List.of(t));

        mvc.perform(get("/tool/by-state")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .param("state", "DAMAGED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].initialState", is("DAMAGED")))
                .andExpect(jsonPath("$[0].available", is(false)));
    }

    @Test
    void listByState_forbidden_whenNotAdmin() throws Exception {
        mvc.perform(get("/tool/by-state")
                        .with(jwt())
                        .param("state", "GOOD"))
                .andExpect(status().isForbidden());
    }


//Direct test, (@WithMockeUser to use @PreAuthorize)


    @Test
    void direct_touchConstructor_forCoverage() {
        new ToolController(Mockito.mock(ToolService.class));
    }

    @WithMockUser(roles = "USER")
    @Test
    void direct_createTool_ok() {
        ToolEntity tool = new ToolEntity();
        tool.setName("Taladro");

        ToolEntity saved = new ToolEntity();
        saved.setId(1L); saved.setName("Taladro");

        given(toolService.saveTool(eq(tool), argThat(u -> "11.111.111-1".equals(u.getRut()))))
                .willReturn(saved);

        var resp = controller.createTool(tool, "11.111.111-1");
        org.junit.jupiter.api.Assertions.assertEquals(200, resp.getStatusCode().value());
        org.junit.jupiter.api.Assertions.assertEquals(1L, ((ToolEntity) resp.getBody()).getId());
    }

    @WithMockUser(roles = "USER")
    @Test
    void direct_createTool_badRequest() {
        given(toolService.saveTool(org.mockito.ArgumentMatchers.any(ToolEntity.class), org.mockito.ArgumentMatchers.any(UserEntity.class)))
                .willThrow(new IllegalArgumentException("duplicate"));

        var resp = controller.createTool(new ToolEntity(), "11.111.111-1");
        org.junit.jupiter.api.Assertions.assertEquals(400, resp.getStatusCode().value());
        org.junit.jupiter.api.Assertions.assertTrue(resp.getBody().toString().contains("duplicate"));
    }

    @WithMockUser(roles = "ADMIN")
    @Test
    void direct_updateTool_ok_paramsNull() {
        ToolEntity updated = new ToolEntity();
        updated.setId(9L); updated.setName("Sierra");

        given(toolService.updateTool(eq(9L), isNull(), isNull(), isNull(),
                argThat(u -> "11.111.111-1".equals(u.getRut()))))
                .willReturn(updated);

        var resp = controller.updateTool(9L, null, null, null, "11.111.111-1");
        org.junit.jupiter.api.Assertions.assertEquals(200, resp.getStatusCode().value());
        org.junit.jupiter.api.Assertions.assertEquals(9L, ((ToolEntity) resp.getBody()).getId());
    }

    @WithMockUser(roles = "ADMIN")
    @Test
    void direct_updateTool_badRequest() {
        given(toolService.updateTool(eq(8L), eq("XXX"), eq(1), isNull(),org.mockito.ArgumentMatchers.any(UserEntity.class)))
                .willThrow(new IllegalArgumentException("Invalid state"));

        var resp = controller.updateTool(8L, "XXX", 1, null, "22.222.222-2");
        org.junit.jupiter.api.Assertions.assertEquals(400, resp.getStatusCode().value());
        org.junit.jupiter.api.Assertions.assertTrue(resp.getBody().toString().contains("Invalid state"));
    }

    @WithMockUser(roles = "USER")
    @Test
    void direct_listNamesWithCategory_ok() {
        given(toolService.getAllNamesWithCategory()).willReturn(List.of());
        var resp = controller.listNamesWithCategory();
        org.junit.jupiter.api.Assertions.assertEquals(200, resp.getStatusCode().value());
    }

    @WithMockUser(roles = "USER")
    @Test
    void direct_listAvailable_ok() {
        given(toolService.listAvailable()).willReturn(List.of(new ToolEntity()));
        var resp = controller.listAvailable();
        org.junit.jupiter.api.Assertions.assertEquals(200, resp.getStatusCode().value());
    }

    @WithMockUser(roles = "ADMIN")
    @Test
    void direct_listByState_ok() {
        given(toolService.listByState("DAMAGED")).willReturn(List.of());
        var resp = controller.listByState("DAMAGED");
        org.junit.jupiter.api.Assertions.assertEquals(200, resp.getStatusCode().value());
    }
}
