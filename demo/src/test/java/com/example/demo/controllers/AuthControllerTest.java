package com.example.demo.controllers;

import com.example.demo.entities.UserEntity;
import com.example.demo.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserService userService;

    @Test
    void me_ok_whenAuthenticated_returnsUser() throws Exception {
        //User's Mock, return service
        UserEntity u = new UserEntity();
        u.setId(1L);
        u.setRut("11.111.111-1");
        u.setName("Ana");
        u.setActive(true);

        given(userService.provisionFromJwt(any(Jwt.class))).willReturn(u);

        //Simulated jwt
        Jwt myJwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("sub", "user-1")
                .claim("preferred_username", "ana")
                .claim("email", "ana@example.com")
                .build();

        mockMvc.perform(get("/auth/me").with(jwt().jwt(myJwt)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Ana")))
                .andExpect(jsonPath("$.rut", is("11.111.111-1")))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    void me_unauthenticated_returns401() throws Exception {
        //without .with(jwt()), should blocked by @PreAuthorize("isAuthenticated()")
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
