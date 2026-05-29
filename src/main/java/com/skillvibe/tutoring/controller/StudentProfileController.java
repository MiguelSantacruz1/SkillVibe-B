package com.skillvibe.tutoring.controller;

import com.skillvibe.tutoring.dto.ApiResponse;
import com.skillvibe.tutoring.model.StudentProfile;
import com.skillvibe.tutoring.security.UserPrincipal;
import com.skillvibe.tutoring.service.StudentProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
@Tag(name = "Perfil de Estudiante", description = "Endpoints para gestionar el perfil del estudiante")
@SecurityRequirement(name = "BearerAuth")
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    public StudentProfileController(StudentProfileService studentProfileService) {
        this.studentProfileService = studentProfileService;
    }

    @Operation(summary = "Obtener perfil del estudiante autenticado")
    @GetMapping("/profile")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<StudentProfile>> getProfile(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        StudentProfile profile = studentProfileService.getOrCreateProfile(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Perfil obtenido", profile));
    }

    @Operation(summary = "Actualizar perfil del estudiante")
    @PutMapping("/profile")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<StudentProfile>> updateProfile(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String bio = (String) body.get("bio");
        String profilePictureUrl = (String) body.get("profilePictureUrl");
        @SuppressWarnings("unchecked")
        List<String> interests = (List<String>) body.get("interests");
        StudentProfile updated = studentProfileService.updateProfile(
                principal.getId(), bio, profilePictureUrl, interests);
        return ResponseEntity.ok(ApiResponse.success("Perfil actualizado con éxito", updated));
    }
}
