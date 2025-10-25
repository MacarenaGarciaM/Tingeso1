package com.example.demo.controllers;

import com.example.demo.entities.LoanEntity;
import com.example.demo.repositories.LoanItemRepository;
import com.example.demo.repositories.LoanRepository;
import com.example.demo.services.LoanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoanController.class)
@Import(LoanControllerTest.MethodSecurityCfg.class)
class LoanControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityCfg {}

    @Autowired MockMvc mvc;

    @MockitoBean
    LoanService loanService;
    @MockitoBean LoanItemRepository loanItemRepository;
    @MockitoBean LoanRepository loanRepository;

    // POST /loan
    @Test
    void createLoan_ok_userRole() throws Exception {
        LoanEntity created = new LoanEntity(); created.setId(10L);

        given(loanService.createLoan(eq("11.111.111-1"),
                eq(LocalDate.parse("2025-10-01")),
                eq(LocalDate.parse("2025-10-04")),
                anyList())).willReturn(created);

        mvc.perform(post("/loan")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .param("rutUser","11.111.111-1")
                        .param("reservationDate","2025-10-01")
                        .param("returnDate","2025-10-04")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)));
    }

    @Test
    void createLoan_badRequest_whenServiceThrows() throws Exception {
        given(loanService.createLoan(anyString(), any(), any(), anyList()))
                .willThrow(new IllegalArgumentException("Inactive user"));

        mvc.perform(post("/loan")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .param("rutUser","11.111.111-1")
                        .param("reservationDate","2025-10-01")
                        .param("returnDate","2025-10-04")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Inactive user")));
    }

    // POST /loan/{id}/return (ADMIN)
    @Test
    void returnLoan_ok_adminRole_mapsOutput() throws Exception {
        LoanEntity updated = new LoanEntity();
        updated.setId(7L);
        updated.setLateFine(900);
        updated.setDamagePenalty(1200);

        given(loanService.returnLoan(eq(7L),
                eq(LocalDate.parse("2025-10-05")),
                anySet(), anySet(), isNull(), anyMap()))
                .willReturn(updated);

        String body = """
          {
            "actualReturnDate":"2025-10-05",
            "damaged":[1],
            "irreparable":[],
            "damagedCosts":{"1":1200}
          }
          """;

        mvc.perform(post("/loan/{loanId}/return", 7L)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(7)))
                .andExpect(jsonPath("$.lateFine", is(900)))
                .andExpect(jsonPath("$.damagePenalty", is(1200)));
    }

    @Test
    void returnLoan_missingDate_returns400() throws Exception {
        mvc.perform(post("/loan/{loanId}/return", 1L)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"damaged\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("actualReturnDate")));
    }

    @Test
    void returnLoan_invalidDate_returns400() throws Exception {
        mvc.perform(post("/loan/{loanId}/return", 1L)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actualReturnDate\":\"xxx\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid date")));
    }

    // finePerDay as String and damaged/irreparable as loose value
    @Test
    void returnLoan_stringFinePerDay_andScalarSets_ok() throws Exception {
        LoanEntity updated = new LoanEntity(); updated.setId(5L);

        given(loanService.returnLoan(eq(5L),
                eq(LocalDate.parse("2025-10-06")),
                argThat(s -> s.contains(3L)),
                argThat(s -> s.contains(4L)),
                eq(5), // finePerDay parsed from "5"
                anyMap()))
                .willReturn(updated);

        String body = """
          {
            "actualReturnDate":"2025-10-06",
            "finePerDay":"5",
            "damaged":3,
            "irreparable":"4"
          }
          """;

        mvc.perform(post("/loan/{loanId}/return", 5L)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(5)));
    }

    @Test
    void returnLoan_serviceThrowsIAE_returns400() throws Exception {
        given(loanService.returnLoan(anyLong(), any(), anySet(), anySet(), any(), anyMap()))
                .willThrow(new IllegalArgumentException("bad state"));

        mvc.perform(post("/loan/{loanId}/return", 1L)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actualReturnDate\":\"2025-10-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("bad state")));
    }

    @Test
    void returnLoan_serviceThrowsRuntime_returns500() throws Exception {
        given(loanService.returnLoan(anyLong(), any(), anySet(), anySet(), any(), anyMap()))
                .willThrow(new RuntimeException("boom"));

        mvc.perform(post("/loan/{loanId}/return", 1L)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actualReturnDate\":\"2025-10-01\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("boom")));
    }

    // POST /loan/{id}/pay-fines
    @Test
    void payFines_ok_adminRole() throws Exception {
        LoanEntity after = new LoanEntity(); after.setId(3L);
        given(loanService.payFines(3L, true, false)).willReturn(after);

        mvc.perform(post("/loan/{loanId}/pay-fines", 3L)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payLateFine\":true,\"payDamagePenalty\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(3)));
    }

    //GET /loan/active
    @Test
    void listActive_admin_noRut_callsAllActive() throws Exception {
        given(loanService.listAllActiveLoans()).willReturn(List.of());

        mvc.perform(get("/loan/active")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(loanService).listAllActiveLoans();
    }

    @Test
    void listActive_user_noRut_returns400() throws Exception {
        mvc.perform(get("/loan/active")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listActive_withRut_callsByRut() throws Exception {
        given(loanService.listActiveLoans("11.111.111-1")).willReturn(List.of());

        mvc.perform(get("/loan/active")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .param("rutUser","11.111.111-1"))
                .andExpect(status().isOk());

        verify(loanService).listActiveLoans("11.111.111-1");
    }
    @Test
    void listActive_admin_blankRut_usesAllActive() throws Exception {
        given(loanService.listAllActiveLoans()).willReturn(List.of());
        mvc.perform(get("/loan/active")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .param("rutUser"," "))
                .andExpect(status().isOk());
        verify(loanService).listAllActiveLoans();
    }

    //GET /loan/top
    @Test
    void topTools_ok_flagsAndMapping() throws Exception {
        List<Object[]> rows = List.of(new Object[]{"Taladro", 5L}, new Object[]{"Sierra", 3L});

        given(loanItemRepository.topByToolName(eq(true), eq(LocalDate.parse("2025-10-01")),
                eq(true), eq(LocalDate.parse("2025-10-31")),
                eq(PageRequest.of(0, 2))))
                .willReturn(rows);

        mvc.perform(get("/loan/top")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .param("limit","2")
                        .param("start","2025-10-01")
                        .param("end","2025-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].tool", is("Taladro")))
                .andExpect(jsonPath("$[0].times", is(5)));
    }

    @Test
    void topTools_defaultLimit_andNoDates() throws Exception {
        given(loanItemRepository.topByToolName(eq(false), isNull(), eq(false), isNull(),
                eq(PageRequest.of(0, 10))))
                .willReturn(List.of());

        mvc.perform(get("/loan/top")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // GET /loan/debts (ADMIN)
    @Test
    void listLoansWithDebts_ok_admin_buildsPageRequestAndNullRut() throws Exception {
        Page<LoanEntity> empty = new PageImpl<>(List.of(),
                PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "reservationDate")), 0);

        given(loanService.listLoansWithUnpaidDebts(
                isNull(),
                eq(LocalDate.parse("2025-10-01")),
                eq(LocalDate.parse("2025-10-31")),
                eq(PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "reservationDate")))
        )).willReturn(empty);

        mvc.perform(get("/loan/debts")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .param("rutUser","")
                        .param("start","2025-10-01")
                        .param("end","2025-10-31")
                        .param("page","0")
                        .param("size","1")
                        .param("sort","reservationDate,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void listLoansWithDebts_asc_andClampedPaging() throws Exception {
        Page<LoanEntity> empty = new PageImpl<>(List.of(),
                PageRequest.of(0, 1, Sort.by(Sort.Direction.ASC, "reservationDate")), 0);

        given(loanService.listLoansWithUnpaidDebts(
                isNull(), isNull(), isNull(),
                eq(PageRequest.of(0, 1, Sort.by(Sort.Direction.ASC, "reservationDate")))
        )).willReturn(empty);

        mvc.perform(get("/loan/debts")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .param("rutUser","")
                        .param("page","-5")
                        .param("size","0")
                        .param("sort","reservationDate,asc"))
                .andExpect(status().isOk());
    }

    // GET /loan/by-rut
    @Test
    void listByRut_ok_buildsPageRequest() throws Exception {
        Page<LoanEntity> pg = new PageImpl<>(List.of(),
                PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "reservationDate")), 0);
        given(loanRepository.findPageByRutUser(eq("11.111.111-1"),
                eq(PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "reservationDate")))))
                .willReturn(pg);

        mvc.perform(get("/loan/by-rut")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .param("rutUser","11.111.111-1")
                        .param("page","0")
                        .param("size","2")
                        .param("sort","reservationDate,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    //GET /loan/overdue
    @Test
    void listOverdue_ok_defaultAscSort() throws Exception {
        Page<LoanEntity> pg = new PageImpl<>(List.of(),
                PageRequest.of(0, 12, Sort.by(Sort.Direction.ASC, "returnDate")), 0);

        given(loanService.listOverdueLoans(isNull(),
                eq(PageRequest.of(0, 12, Sort.by(Sort.Direction.ASC, "returnDate")))))
                .willReturn(pg);

        mvc.perform(get("/loan/overdue")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void listOverdue_desc_withRut() throws Exception {
        Page<LoanEntity> pg = new PageImpl<>(List.of(),
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "returnDate")), 0);

        given(loanService.listOverdueLoans(eq("11.111.111-1"),
                eq(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "returnDate")))))
                .willReturn(pg);

        mvc.perform(get("/loan/overdue")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .param("rutUser","11.111.111-1")
                        .param("size","5")
                        .param("sort","returnDate,desc"))
                .andExpect(status().isOk());
    }
}
