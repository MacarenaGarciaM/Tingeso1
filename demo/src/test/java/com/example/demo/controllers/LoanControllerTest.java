package com.example.demo.controllers;

import com.example.demo.entities.LoanEntity;
import com.example.demo.services.LoanService;
import com.example.demo.repositories.LoanItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoanController.class)
class LoanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoanService loanService;

    @MockitoBean
    private LoanItemRepository loanItemRepository;

    // ====== POST /loan ======
    @Test
    void createLoan_ok() throws Exception {
        LoanEntity created = new LoanEntity();
        created.setId(123L);
        created.setReservationDate(LocalDate.parse("2025-10-01"));
        created.setReturnDate(LocalDate.parse("2025-10-04"));

        given(loanService.createLoan(eq("11.111.111-1"),
                eq(LocalDate.parse("2025-10-01")),
                eq(LocalDate.parse("2025-10-04")),
                anyList()))
                .willReturn(created);

        String itemsJson = "[]"; // cuerpo mínimo válido (lista vacía de items)

        mockMvc.perform(post("/loan")
                        .param("rutUser", "11.111.111-1")
                        .param("reservationDate", "2025-10-01")
                        .param("returnDate", "2025-10-04")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemsJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(123)));
    }

    @Test
    void createLoan_badRequest_whenServiceThrowsIAE() throws Exception {
        given(loanService.createLoan(anyString(), any(), any(), anyList()))
                .willThrow(new IllegalArgumentException("User inactive"));

        mockMvc.perform(post("/loan")
                        .param("rutUser", "11.111.111-1")
                        .param("reservationDate", "2025-10-01")
                        .param("returnDate", "2025-10-04")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("User inactive")));
    }

    // ====== POST /loan/{id}/return ======
    @Test
    void returnLoan_ok() throws Exception {
        LoanEntity updated = new LoanEntity();
        updated.setId(9L);

        given(loanService.returnLoan(eq(9L),
                eq(LocalDate.parse("2025-10-05")),
                anySet(), anySet(), isNull()))
                .willReturn(updated);

        String body = """
            {
              "actualReturnDate": "2025-10-05",
              "damaged": [1,2],
              "irreparable": []
            }
            """;

        mockMvc.perform(post("/loan/{loanId}/return", 9L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(9)));
    }

    @Test
    void returnLoan_badRequest_whenServiceThrowsIAE() throws Exception {
        given(loanService.returnLoan(anyLong(), any(), anySet(), anySet(), any()))
                .willThrow(new IllegalArgumentException("Invalid state"));

        String body = """
            { "actualReturnDate": "2025-10-05" }
            """;

        mockMvc.perform(post("/loan/{loanId}/return", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid state")));
    }

    // ====== POST /loan/{id}/pay-fines ======
    @Test
    void payFines_ok() throws Exception {
        LoanEntity updated = new LoanEntity();
        updated.setId(7L);

        given(loanService.payFines(7L, true, false)).willReturn(updated);

        String body = """
            { "payLateFine": true, "payDamagePenalty": false }
            """;

        mockMvc.perform(post("/loan/{loanId}/pay-fines", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(7)));
    }

    @Test
    void payFines_badRequest_whenServiceThrowsIAE() throws Exception {
        given(loanService.payFines(anyLong(), anyBoolean(), anyBoolean()))
                .willThrow(new IllegalArgumentException("No fines to pay"));

        String body = """
            { "payLateFine": false, "payDamagePenalty": false }
            """;

        mockMvc.perform(post("/loan/{loanId}/pay-fines", 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("No fines to pay")));
    }

    // ====== GET /loan/top ======
    @Test
    void topTools_ok_mapsRows() throws Exception {
        // Simulamos que el repo devuelve: [["Taladro", 5], ["Sierra", 3]]
        List<Object[]> rows = List.of(
                new Object[]{"Taladro", 5L},
                new Object[]{"Sierra", 3L}
        );
        // start/end vienen parseados por @DateTimeFormat
        given(loanItemRepository.topByToolName(
                eq(LocalDate.parse("2025-10-01")),
                eq(LocalDate.parse("2025-10-31")),
                eq(PageRequest.of(0, 2))))
                .willReturn(rows);

        ResultActions res = mockMvc.perform(get("/loan/top")
                .param("limit", "2")
                .param("start", "2025-10-01")
                .param("end", "2025-10-31"));

        res.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].tool", is("Taladro")))
                .andExpect(jsonPath("$[0].times", is(5)))
                .andExpect(jsonPath("$[1].tool", is("Sierra")))
                .andExpect(jsonPath("$[1].times", is(3)));
    }

    @Test
    void topTools_ok_defaultLimitAndNullDates() throws Exception {
        // Cuando limit no viene, el controller usa size=10 y start/end = null
        given(loanItemRepository.topByToolName(
                isNull(), isNull(), eq(PageRequest.of(0, 10))))
                .willReturn(List.of());

        mockMvc.perform(get("/loan/top"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
