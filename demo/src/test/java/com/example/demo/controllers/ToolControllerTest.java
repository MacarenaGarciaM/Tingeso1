package com.example.demo.controllers;

import com.example.demo.entities.ToolEntity;
import com.example.demo.entities.UserEntity;
import com.example.demo.services.ToolService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ToolController.class)
class ToolControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ToolService toolService;

    // -------- POST /tool (createTool) --------
    @Test
    void createTool_ok() throws Exception {
        ToolEntity saved = new ToolEntity();
        saved.setId(100L);
        saved.setName("Taladro");
        saved.setCategory("Eléctricas");
        saved.setInitialState("Disponible");
        saved.setRepositionValue(50000);
        saved.setAvailable(true);
        saved.setAmount(5);

        given(toolService.saveTool(any(ToolEntity.class), any(UserEntity.class)))
                .willReturn(saved);

        String body = """
        {
          "tool": {
            "name": "Taladro",
            "category": "Eléctricas",
            "initialState": "Disponible",
            "repositionValue": 50000,
            "available": true,
            "amount": 5
          },
          "user": {
            "id": 1,
            "rut": "11.111.111-1",
            "name": "Ana",
            "active": true
          }
        }
        """;

        mockMvc.perform(post("/tool")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(100)))
                .andExpect(jsonPath("$.name", is("Taladro")))
                .andExpect(jsonPath("$.category", is("Eléctricas")))
                .andExpect(jsonPath("$.amount", is(5)))
                .andExpect(jsonPath("$.available", is(true)));
    }

    @Test
    void createTool_badRequest_whenServiceThrowsIAE() throws Exception {
        given(toolService.saveTool(any(ToolEntity.class), any(UserEntity.class)))
                .willThrow(new IllegalArgumentException("Tool already exists"));

        String body = """
        {
          "tool": {
            "name": "Taladro",
            "category": "Eléctricas",
            "initialState": "Disponible",
            "repositionValue": 50000,
            "available": true,
            "amount": 5
          },
          "user": {
            "id": 1,
            "rut": "11.111.111-1",
            "name": "Ana",
            "active": true
          }
        }
        """;

        mockMvc.perform(post("/tool")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Tool already exists")));
    }

    // -------- PUT /tool/{id} (updateTool) --------
    @Test
    void updateTool_ok() throws Exception {
        ToolEntity updated = new ToolEntity();
        updated.setId(5L);
        updated.setName("Sierra");
        updated.setCategory("Manuales");
        updated.setInitialState("Reparación");
        updated.setRepositionValue(20000);
        updated.setAvailable(true);
        updated.setAmount(7);

        // El controller pasa (id, state, amount, user)
        given(toolService.updateTool(eq(5L), eq("Reparación"), eq(7), any(UserEntity.class)))
                .willReturn(updated);

        String userJson = """
        {"id":1,"rut":"11.111.111-1","name":"Ana","active":true}
        """;

        mockMvc.perform(put("/tool/{id}", 5L)
                        .param("state", "Reparación")
                        .param("amount", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(5)))
                .andExpect(jsonPath("$.initialState", is("Reparación")))
                .andExpect(jsonPath("$.amount", is(7)));
    }

    @Test
    void updateTool_badRequest_whenServiceThrowsIAE() throws Exception {
        doThrow(new IllegalArgumentException("Invalid state"))
                .when(toolService).updateTool(eq(8L), eq("XXX"), eq(1), any(UserEntity.class));

        String userJson = """
        {"id":2,"rut":"22.222.222-2","name":"Bruno","active":true}
        """;

        mockMvc.perform(put("/tool/{id}", 8L)
                        .param("state", "XXX")
                        .param("amount", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid state")));
    }
}
