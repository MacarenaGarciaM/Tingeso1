package com.example.demo.controllers;

import com.example.demo.services.SettingService;
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

import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SettingController.class)
@Import(SettingControllerTest.MethodSecurityCfg.class)
class SettingControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityCfg {}

    @Autowired
    MockMvc mvc;

    @MockitoBean
    SettingService settingService;

    //GET /settings/daily-rate
    @Test
    void getDailyRate_ok_authenticated() throws Exception {
        given(settingService.getDailyRentPrice()).willReturn(1500);

        mvc.perform(get("/settings/daily-rate")
                        .with(jwt())) // autenticado (aunque el endpoint no tiene @PreAuthorize)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.value", is(1500)));

        verify(settingService).getDailyRentPrice();
    }

    //PUT /settings/daily-rate (ADMIN)
    @Test
    void updateDailyRate_ok_adminRole_numericBody() throws Exception {
        given(settingService.setDailyRentPrice(2500)).willReturn(2500);

        mvc.perform(put("/settings/daily-rate")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":2500}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value", is(2500)));

        verify(settingService).setDailyRentPrice(2500);
    }

    @Test
    void updateDailyRate_badRequest_missingValue() throws Exception {
        mvc.perform(put("/settings/daily-rate")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("value is required")));
    }

    @Test
    void updateDailyRate_forbidden_whenNotAdmin() throws Exception {
        mvc.perform(put("/settings/daily-rate")
                        .with(jwt()) // autenticado pero sin ROLE_ADMIN
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":2000}"))
                .andExpect(status().isForbidden());
    }
}
