package com.example.demo.controllers;

import com.example.demo.entities.KardexEntity;
import com.example.demo.services.KardexService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(KardexController.class)
@Import(KardexControllerTest.MethodSecurityTestConfig.class)
class KardexControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    KardexService kardexService;

    @TestConfiguration
    @EnableMethodSecurity //Enables @PreAuthorize at the slice
    static class MethodSecurityTestConfig { }

    @Test
    void list_ok_asAdmin_returnsPageAndBuildsPageRequest() throws Exception {
        // Page mock
        KardexEntity e1 = new KardexEntity();
        e1.setId(1L);
        e1.setMovementDate(LocalDate.parse("2025-10-01"));
        e1.setType("LOAN");

        KardexEntity e2 = new KardexEntity();
        e2.setId(2L);
        e2.setMovementDate(LocalDate.parse("2025-10-02"));
        e2.setType("RETURN");

        Page<KardexEntity> page = new PageImpl<>(List.of(e1, e2), PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "movementDate")), 2);

        //Controller should normalize void to null and create PageRequest
        given(kardexService.search(
                eq(5L),
                isNull(), // rutUser="" -> null
                eq("LOAN"),
                eq(LocalDate.parse("2025-10-01")),
                eq(LocalDate.parse("2025-10-31")),
                isNull(), // name="" -> null
                eq("Electricidad"),
                eq(PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "movementDate")))
        )).willReturn(page);

        mockMvc.perform(get("/kardex")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .param("toolId", "5")
                        .param("rutUser", "") // should normalize to null
                        .param("type", "LOAN")
                        .param("start", "2025-10-01")
                        .param("end", "2025-10-31")
                        .param("name", "") // should normalize to null
                        .param("category", "Electricidad")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "movementDate,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))

                //Spring serialize Page as object with "content", "totalElements", etc.
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[0].type", is("LOAN")))
                .andExpect(jsonPath("$.totalElements", is(2)));

        //verify PageRequest
        verify(kardexService).search(
                eq(5L), isNull(), eq("LOAN"),
                eq(LocalDate.parse("2025-10-01")),
                eq(LocalDate.parse("2025-10-31")),
                isNull(), eq("Electricidad"),
                eq(PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "movementDate")))
        );
    }

    @Test
    void list_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/kardex"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_authenticatedWithoutAdminRole_returns403() throws Exception {

        //User authenticated without ROLE_ADMIN
        mockMvc.perform(get("/kardex")
                        .with(jwt())) // without authorities doesnt have ROLE_ADMIN
                .andExpect(status().isForbidden());
    }

    @Test
    void list_ok_sortAsc_andClampPageSize() throws Exception {
        Page<KardexEntity> empty = new PageImpl<>(List.of(),
                PageRequest.of(0, 1, Sort.by(Sort.Direction.ASC, "movementDate")), 0);

        given(kardexService.search(
                isNull(), eq("11.111.111-1"), isNull(),
                isNull(), isNull(), eq("Taladro"), isNull(),
                eq(PageRequest.of(0, 1, Sort.by(Sort.Direction.ASC, "movementDate")))
        )).willReturn(empty);

        mockMvc.perform(get("/kardex")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .param("rutUser","11.111.111-1")        // branch: not empty
                        .param("name","Taladro")                // branch: not empty
                        .param("page","-3")                     // branch: clamp 0
                        .param("size","0")                      // branch: clamp 1
                        .param("sort","movementDate,asc"))      // branch: asc
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }
}
