package com.skillvibe.tutoring.service;

import com.skillvibe.tutoring.dto.LoginRequest;
import com.skillvibe.tutoring.exception.UnauthorizedException;
import com.skillvibe.tutoring.model.Role;
import com.skillvibe.tutoring.model.User;
import com.skillvibe.tutoring.repository.TutorProfileRepository;
import com.skillvibe.tutoring.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TutorProfileRepository tutorProfileRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPassword("encoded_password");
        testUser.setFullName("Test User");
        testUser.setRole(Role.STUDENT);
        testUser.setEmailVerified(true);

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");
    }

    @Test
    void login_withValidCredentials_returnsUser() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        // Act
        User response = userService.login(loginRequest.getEmail(), loginRequest.getPassword());

        // Assert
        assertNotNull(response);
        assertEquals("Test User", response.getFullName());
    }

    @Test
    void login_withUnknownEmail_throwsUnauthorizedException() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            userService.login(loginRequest.getEmail(), loginRequest.getPassword());
        });
        assertEquals("Credenciales inválidas", exception.getMessage());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_withInvalidPassword_throwsUnauthorizedException() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // Act & Assert
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            userService.login(loginRequest.getEmail(), loginRequest.getPassword());
        });
        assertEquals("Credenciales inválidas", exception.getMessage());
    }
}
