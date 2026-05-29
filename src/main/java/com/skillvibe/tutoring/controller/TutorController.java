package com.skillvibe.tutoring.controller;

import com.skillvibe.tutoring.dto.ApiResponse;
import com.skillvibe.tutoring.dto.TutorProfileResponseDTO;
import com.skillvibe.tutoring.dto.TutorSearchResponseDTO;
import com.skillvibe.tutoring.security.UserPrincipal;
import com.skillvibe.tutoring.service.TutorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/tutor")
@Tag(name = "Tutor Marketplace", description = "Endpoints para búsqueda y visualización de perfiles de tutores")
public class TutorController {

    private final TutorService tutorService;

    public TutorController(TutorService tutorService) {
        this.tutorService = tutorService;
    }

    @Operation(summary = "Buscar tutores con filtros dinámicos", description = "Permite buscar por nombre, materia, rango de precio y experiencia.")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<TutorSearchResponseDTO>>> searchTutors(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Integer minExperience,
            @RequestParam(required = false, defaultValue = "true") Boolean onlyVerified,
            @PageableDefault(size = 12) Pageable pageable
    ) {
        Page<TutorSearchResponseDTO> result = tutorService.searchTutors(
                query, subject, minPrice, maxPrice, minExperience, onlyVerified, pageable
        );
        return ResponseEntity.ok(ApiResponse.success("Búsqueda completada", result));
    }

    @Operation(summary = "Obtener mi perfil de tutor", description = "Solo accesible para usuarios con rol TUTOR.")
    @GetMapping("/profile")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<ApiResponse<TutorProfileResponseDTO>> getMyProfile(
            org.springframework.security.core.Authentication authentication
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        // Fix #1: Usar DTO en vez de la entidad JPA para evitar LazyInitializationException
        return ResponseEntity.ok(ApiResponse.success("Perfil obtenido", new TutorProfileResponseDTO(tutorService.getProfileByUserId(principal.getId()))));
    }

    @Operation(summary = "Actualizar mi perfil de tutor", description = "Solo accesible para usuarios con rol TUTOR.")
    @PutMapping("/profile")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<ApiResponse<TutorProfileResponseDTO>> updateProfile(
            @RequestBody com.skillvibe.tutoring.dto.TutorProfileUpdateDTO updateDTO,
            org.springframework.security.core.Authentication authentication
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        // Fix #1: Usar DTO en vez de la entidad JPA para evitar LazyInitializationException
        return ResponseEntity.ok(ApiResponse.success("Perfil actualizado con éxito", new TutorProfileResponseDTO(tutorService.updateProfile(principal.getId(), updateDTO))));
    }

    @GetMapping("/panel")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<ApiResponse<String>> privateZone() {
        String mensaje = tutorService.getTutorPanelMessage();
        return ResponseEntity.ok(ApiResponse.success(mensaje));
    }
}