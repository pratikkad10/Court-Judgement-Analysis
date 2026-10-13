package com.judgment.auth.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.judgment.auth.dto.AuthDtos.*;
import com.judgment.auth.service.AuthService;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthControllerTest {
  @Test
  void loginUsesStandardEnvelope() throws Exception {
    AuthService service = mock(AuthService.class);
    when(service.login(any()))
        .thenReturn(
            new TokenResponse(
                "access",
                "refresh",
                "Bearer",
                900,
                new UserView(UUID.randomUUID(), "user@example.com", "USER", true)));
    MockMvc mvc = MockMvcBuilders.standaloneSetup(new AuthController(service)).build();
    mvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\",\"password\":\"secure-password\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.accessToken").value("access"));
  }
}
