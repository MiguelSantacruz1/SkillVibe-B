package com.skillvibe.tutoring.controller;

import com.skillvibe.tutoring.model.Tutoria;
import com.skillvibe.tutoring.service.TutoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tutorias")
@Tag(name = "Tutorías", description = "Endpoints para la gestión de clases y pagos de SkillVibe")
@SecurityRequirement(name = "BearerAuth") // Para que Swagger pida el Token
public class TutoriaController {

    private final TutoriaService tutoriaService;

    public TutoriaController(TutoriaService tutoriaService) {
        this.tutoriaService = tutoriaService;
    }

    @Operation(summary = "Programar una nueva clase (Manual)", description = "Solo accesible para usuarios con rol TUTOR. Descuenta el precio del balance del estudiante.")
    @PostMapping("/programar")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<com.skillvibe.tutoring.dto.ApiResponse<Tutoria>> crearTutoria(@RequestBody Tutoria tutoria) {
        return ResponseEntity.ok(com.skillvibe.tutoring.dto.ApiResponse.success("Tutoría programada con éxito", tutoriaService.guardarTutoria(tutoria)));
    }

    @Operation(summary = "Reservar una tutoría (Estudiante)", description = "Permite a un estudiante reservar una clase. Valida saldo y obtiene el precio del perfil del tutor.")
    @PostMapping("/reservar")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<com.skillvibe.tutoring.dto.ApiResponse<Tutoria>> reservar(
            @RequestBody com.skillvibe.tutoring.dto.BookingRequestDTO request,
            org.springframework.security.core.Authentication authentication
    ) {
        // Obtenemos el usuario autenticado (asumiendo que el Principal es el User de nuestro modelo o tiene el ID)
        com.skillvibe.tutoring.model.User currentUser = (com.skillvibe.tutoring.model.User) authentication.getPrincipal();
        return ResponseEntity.ok(com.skillvibe.tutoring.dto.ApiResponse.success("Reserva realizada con éxito", tutoriaService.reservarTutoria(currentUser.getId(), request)));
    }

    @Operation(summary = "Ver el tablero de actividades", description = "Muestra todas las tutorías asociadas a un usuario (como tutor o como estudiante).")
    @GetMapping("/mi-tablero/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<com.skillvibe.tutoring.dto.ApiResponse<List<Tutoria>>> obtenerTablero(@PathVariable Long userId) {
        return ResponseEntity.ok(com.skillvibe.tutoring.dto.ApiResponse.success("Tablero obtenido con éxito", tutoriaService.listarPorUsuario(userId)));
    }

    @Operation(summary = "Finalizar clase y pagar al tutor", description = "Cambia el estado a FINALIZADA y suma el valor de la clase al balance del tutor.")
    @PutMapping("/{id}/finalizar")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<com.skillvibe.tutoring.dto.ApiResponse<Tutoria>> finalizar(@PathVariable Long id) {
        return ResponseEntity.ok(com.skillvibe.tutoring.dto.ApiResponse.success("Clase finalizada correctamente", tutoriaService.finalizarTutoria(id)));
    }
}