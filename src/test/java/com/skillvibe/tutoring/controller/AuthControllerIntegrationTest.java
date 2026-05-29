package com.skillvibe.tutoring.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillvibe.tutoring.dto.LoginRequest;
import com.skillvibe.tutoring.dto.RegisterRequest;
import com.skillvibe.tutoring.model.User;
import com.skillvibe.tutoring.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // Rollback after each test
@SuppressWarnings("null")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Clean up database (optional due to @Transactional, but good practice)
        userRepository.deleteAll();

        // Create a test user
        User user = new User();
        user.setEmail("integration@skillvibe.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setFullName("Integration Test");
        user.setRole(com.skillvibe.tutoring.model.Role.STUDENT);
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    @Test
    void login_withValidCredentials_returns200AndToken() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("integration@skillvibe.com");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.user.email").value("integration@skillvibe.com"));
    }

    @Test
    void login_withInvalidCredentials_returns401() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("integration@skillvibe.com");
        loginRequest.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized()) // 401
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"));
    }

    @Test
    void registerUser_withNewEmail_returns200() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("newuser@skillvibe.com");
        registerRequest.setPassword("securepass");
        registerRequest.setFullName("New User");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("newuser@skillvibe.com"));
    }

    @Test
    void registerUser_withExistingEmail_returns400() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("integration@skillvibe.com"); // Already exists
        registerRequest.setPassword("securepass");
        registerRequest.setFullName("Dupe User");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest()) // 400
                .andExpect(jsonPath("$.success").value(false));
    }
}
