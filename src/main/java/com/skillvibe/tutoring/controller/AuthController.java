package com.skillvibe.tutoring.controller;

import com.skillvibe.tutoring.dto.ApiResponse;
import com.skillvibe.tutoring.dto.AuthResponseDTO;
import com.skillvibe.tutoring.dto.LoginRequest;
import com.skillvibe.tutoring.dto.RegisterRequest;
import com.skillvibe.tutoring.dto.TutorRegistrationRequest;
import com.skillvibe.tutoring.dto.UserResponseDTO;
import com.skillvibe.tutoring.model.TutorProfile;
import com.skillvibe.tutoring.model.User;
import com.skillvibe.tutoring.security.JwtService;
import com.skillvibe.tutoring.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponseDTO>> register(@Valid @RequestBody RegisterRequest request) {
        User newUser = userService.registerUser(request);
        return ResponseEntity.ok(ApiResponse.success("Usuario registrado exitosamente", new UserResponseDTO(newUser)));
    }

    @PostMapping("/register/tutor")
    public ResponseEntity<ApiResponse<TutorProfile>> registerTutor(@Valid @RequestBody TutorRegistrationRequest request) {
        TutorProfile newTutor = userService.registerTutor(request);
        return ResponseEntity.ok(ApiResponse.success("Tutor registrado exitosamente. Pendiente de verificación.", newTutor));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> login(@Valid @RequestBody LoginRequest loginRequest) {
        // 1. Validamos credenciales
        User user = userService.login(loginRequest.getEmail(), loginRequest.getPassword());

        // 2. Generamos el Token
        String token = jwtService.generateToken(user);

        // 3. Devolvemos respuesta completa
        AuthResponseDTO responseDTO = new AuthResponseDTO(token, new UserResponseDTO(user));
        return ResponseEntity.ok(ApiResponse.success("Login exitoso", responseDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getUser(@PathVariable Long id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(ApiResponse.success("Usuario obtenido", new UserResponseDTO(user)));
    }

    @GetMapping("/perfil-estudiante")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<String>> zonaEstudiantes() {
        return ResponseEntity.ok(ApiResponse.success("¡Acceso concedido! Hola Andres, este es tu panel de estudiante.", null));
    }
}