package com.skillvibe.tutoring.controller;

import com.skillvibe.tutoring.dto.AdminVerifyDTO;
import com.skillvibe.tutoring.dto.ApiResponse;
import com.skillvibe.tutoring.dto.TutorProfileResponseDTO;
import com.skillvibe.tutoring.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Endpoints para la administración del sistema (solo ROLE_ADMIN)")
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @Operation(summary = "Obtener tutores pendientes", description = "Lista todos los perfiles de tutores que aún no han sido verificados.")
    @GetMapping("/tutors/pending")
    public ResponseEntity<ApiResponse<List<TutorProfileResponseDTO>>> getPendingTutors() {
        return ResponseEntity.ok(ApiResponse.success("Tutores pendientes obtenidos", adminService.getPendingTutors()));
    }

    @Operation(summary = "Obtener tutores verificados", description = "Lista todos los perfiles de tutores verificados.")
    @GetMapping("/tutors/verified")
    public ResponseEntity<ApiResponse<List<TutorProfileResponseDTO>>> getVerifiedTutors() {
        return ResponseEntity.ok(ApiResponse.success("Tutores verificados obtenidos", adminService.getVerifiedTutors()));
    }

    @Operation(summary = "Aprobar o rechazar un tutor", description = "Cambia el estado isVerified de un perfil de tutor y le envía una notificación.")
    @PutMapping("/tutors/{id}/verify")
    public ResponseEntity<ApiResponse<TutorProfileResponseDTO>> verifyTutor(
            @PathVariable Long id,
            @Valid @RequestBody AdminVerifyDTO dto
    ) {
        TutorProfileResponseDTO response = adminService.verifyTutor(id, dto);
        String msg = dto.getVerified() ? "Tutor verificado exitosamente" : "Solicitud de tutor rechazada";
        return ResponseEntity.ok(ApiResponse.success(msg, response));
    }

    @Operation(summary = "Estadísticas del sistema", description = "Obtiene conteos generales de usuarios, tutores y tutorías.")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemStats() {
        return ResponseEntity.ok(ApiResponse.success("Estadísticas obtenidas", adminService.getSystemStats()));
    }
}
