package com.skillvibe.tutoring.controller;

import com.skillvibe.tutoring.model.TutoringClass;
import com.skillvibe.tutoring.service.TutoringClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.skillvibe.tutoring.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tutoringClasses")
@Tag(name = "Tutorías", description = "Endpoints para la gestión de clases y pagos de SkillVibe")
@SecurityRequirement(name = "BearerAuth") // Para que Swagger pida el Token
public class TutoringClassController {

    private final TutoringClassService TutoringClassService;

    public TutoringClassController(TutoringClassService TutoringClassService) {
        this.TutoringClassService = TutoringClassService;
    }

    @Operation(summary = "Programar una nueva clase (Manual)", description = "Solo accesible para usuarios con rol TUTOR. Descuenta el precio del balance del estudiante.")
    @PostMapping("/programar")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<com.skillvibe.tutoring.dto.ApiResponse<TutoringClass>> createTutoringClass(@RequestBody TutoringClass TutoringClass) {
        return ResponseEntity.ok(com.skillvibe.tutoring.dto.ApiResponse.success("Tutoría PROGRAMMED con éxito", TutoringClassService.saveClass(TutoringClass)));
    }

    @PostMapping("/reservar")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<com.skillvibe.tutoring.dto.ApiResponse<TutoringClass>> bookClass(
            @RequestBody com.skillvibe.tutoring.dto.BookingRequestDTO request,
            org.springframework.security.core.Authentication authentication
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(com.skillvibe.tutoring.dto.ApiResponse.success("Reserva realizada con éxito", TutoringClassService.bookClass(principal.getId(), request)));
    }

    @Operation(summary = "Ver el tablero de actividades", description = "Muestra todas las tutorías asociadas a un usuario (como tutor o como estudiante).")
    @GetMapping("/mi-tablero/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<com.skillvibe.tutoring.dto.ApiResponse<List<TutoringClass>>> getDashboard(@PathVariable Long userId) {
        return ResponseEntity.ok(com.skillvibe.tutoring.dto.ApiResponse.success("Tablero obtenido con éxito", TutoringClassService.listByUser(userId)));
    }

    @Operation(summary = "Finalizar clase y pagar al tutor", description = "Cambia el estado a COMPLETED y suma el valor de la clase al balance del tutor.")
    @PutMapping("/{id}/finalizar")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<com.skillvibe.tutoring.dto.ApiResponse<TutoringClass>> finishClass(
            @PathVariable Long id,
            org.springframework.security.core.Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(com.skillvibe.tutoring.dto.ApiResponse.success("Clase COMPLETED correctamente", TutoringClassService.finishClass(id, principal.getId())));
    }

    @Operation(summary = "Cancelar tutoría con reembolso", description = "Cancela una tutoría en estado PROGRAMMED o IN_PROGRESS y devuelve el saldo al estudiante automáticamente.")
    @PutMapping("/{id}/cancelar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<com.skillvibe.tutoring.dto.ApiResponse<TutoringClass>> cancelClass(
            @PathVariable Long id,
            org.springframework.security.core.Authentication authentication) {
        com.skillvibe.tutoring.security.UserPrincipal principal =
                (com.skillvibe.tutoring.security.UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(com.skillvibe.tutoring.dto.ApiResponse.success(
                "Tutoría CANCELLED. El saldo ha sido reembolsado.",
                TutoringClassService.cancelClass(id, principal.getId())
        ));
    }
}