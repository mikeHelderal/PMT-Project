package com.exercice.pmt.controller;

import com.exercice.pmt.model.User;
import com.exercice.pmt.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController - Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("john_doe");
        user.setEmail("john@example.com");
        user.setPassword("password123");
    }

    // ---------------------------------------------------------------
    // POST /api/auth/register
    // ---------------------------------------------------------------

    @Test
    @DisplayName("POST /register - 200 : inscription réussie")
    void register_success_returns200() throws Exception {
        when(userService.userInscription(any(User.class))).thenReturn(user);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.username").value("john_doe"));

        verify(userService).userInscription(any(User.class));
    }

    @Test
    @DisplayName("POST /register - email déjà existant : ServletException wrappant RuntimeException")
    void register_emailAlreadyExists_throwsRuntimeException() {
        when(userService.userInscription(any(User.class)))
                .thenThrow(new RuntimeException("User with email already exists"));

        jakarta.servlet.ServletException thrown = org.junit.jupiter.api.Assertions.assertThrows(
                jakarta.servlet.ServletException.class,
                () -> mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
        );
        org.junit.jupiter.api.Assertions.assertTrue(
                thrown.getCause() instanceof RuntimeException
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                "User with email already exists", thrown.getCause().getMessage()
        );
    }

    // ---------------------------------------------------------------
    // POST /api/auth/login
    // ---------------------------------------------------------------

    @Test
    @DisplayName("POST /login - 200 : connexion réussie")
    void login_success_returns200() throws Exception {
        when(userService.login(eq("john@example.com"), eq("password123"))).thenReturn(user);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.username").value("john_doe"));
    }

    @Test
    @DisplayName("POST /login - identifiants invalides : ServletException wrappant RuntimeException")
    void login_invalidCredentials_throwsRuntimeException() {
        when(userService.login(any(), any()))
                .thenThrow(new RuntimeException("Identifiants invalides"));

        jakarta.servlet.ServletException thrown = org.junit.jupiter.api.Assertions.assertThrows(
                jakarta.servlet.ServletException.class,
                () -> mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
        );
        org.junit.jupiter.api.Assertions.assertTrue(
                thrown.getCause() instanceof RuntimeException
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                "Identifiants invalides", thrown.getCause().getMessage()
        );
    }
}