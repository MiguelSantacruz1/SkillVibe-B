package com.skillvibe.tutoring.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skillvibe.tutoring.dto.ApiResponse;
import com.skillvibe.tutoring.dto.AuthResponseDTO;
import com.skillvibe.tutoring.dto.LoginRequest;
import com.skillvibe.tutoring.dto.RegisterRequest;
import com.skillvibe.tutoring.dto.TutorProfileResponseDTO;
import com.skillvibe.tutoring.dto.TutorRegistrationRequest;
import com.skillvibe.tutoring.dto.UserResponseDTO;
import com.skillvibe.tutoring.model.TutorProfile;
import com.skillvibe.tutoring.model.User;
import com.skillvibe.tutoring.security.JwtService;
import com.skillvibe.tutoring.service.UserService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;

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

    // ── Registration and Login ───────────────────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponseDTO>> register(@Valid @RequestBody RegisterRequest request) {
        User newUser = userService.registerUser(request);
        return ResponseEntity.ok(ApiResponse.success(
                "Cuenta creada. Revisa tu correo para verificar tu cuenta antes de iniciar sesión.",
                new UserResponseDTO(newUser)));
    }

    @PostMapping("/register/tutor")
    public ResponseEntity<ApiResponse<TutorProfileResponseDTO>> registerTutor(@Valid @RequestBody TutorRegistrationRequest request) {
        TutorProfile newTutor = userService.registerTutor(request);
        return ResponseEntity.ok(ApiResponse.success(
                "Tutor registrado. Revisa tu correo para verificar tu cuenta. Tu perfil también estará pendiente de revisión del administrador.",
                new TutorProfileResponseDTO(newTutor)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> login(@Valid @RequestBody LoginRequest loginRequest) {
        User user = userService.login(loginRequest.getEmail(), loginRequest.getPassword());
        String token = jwtService.generateToken(user);
        AuthResponseDTO responseDTO = new AuthResponseDTO(token, new UserResponseDTO(user));
        return ResponseEntity.ok(ApiResponse.success("Login exitoso", responseDTO));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getUser(@PathVariable Long id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(ApiResponse.success("Usuario obtenido", new UserResponseDTO(user)));
    }

    @GetMapping("/perfil-estudiante")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<String>> studentZone() {
        return ResponseEntity.ok(ApiResponse.success("¡Acceso concedido!", null));
    }

    // ── Email Verification ───────────────────────────────────────────────────
    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        userService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success("¡Correo verificado con éxito! Ya puedes iniciar sesión.", null));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@RequestBody EmailRequest body) {
        userService.resendVerification(body.email());
        return ResponseEntity.ok(ApiResponse.success("Se envió un nuevo correo de verificación.", null));
    }

    // ── Password Recovery ────────────────────────────────────────────────────
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody EmailRequest body) {
        userService.requestPasswordReset(body.email());
        return ResponseEntity.ok(ApiResponse.success(
                "Si existe una cuenta con ese correo, recibirás un enlace para restablecer tu contraseña.", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest body) {
        userService.resetPassword(body.token(), body.newPassword());
        return ResponseEntity.ok(ApiResponse.success("¡Contraseña restablecida con éxito! Ya puedes iniciar sesión.", null));
    }

    // ── Auxiliary Records ────────────────────────────────────────────────────
    record EmailRequest(@Email
            @NotBlank String email) {

    }

    record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank
            @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres") String newPassword
            ) {

    }

}
