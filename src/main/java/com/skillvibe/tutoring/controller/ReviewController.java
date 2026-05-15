package com.skillvibe.tutoring.controller;

import com.skillvibe.tutoring.dto.ApiResponse;
import com.skillvibe.tutoring.dto.CreateReviewDTO;
import com.skillvibe.tutoring.dto.ReviewResponseDTO;
import com.skillvibe.tutoring.security.UserPrincipal;
import com.skillvibe.tutoring.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/reviews")
@Tag(name = "Reseñas", description = "Endpoints para calificaciones y reseñas de tutores")
@SecurityRequirement(name = "BearerAuth")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Operation(
        summary = "Crear reseña de una tutoría finalizada",
        description = "Solo el estudiante que cursó la tutoría puede calificarla, y solo una vez."
    )
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<ReviewResponseDTO>> crearReview(
            @Valid @RequestBody CreateReviewDTO dto,
            Authentication authentication
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        ReviewResponseDTO response = reviewService.crearReview(principal.getId(), dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Reseña creada exitosamente", response));
    }

    @Operation(
        summary = "Obtener todas las reseñas de un tutor",
        description = "Devuelve el historial de reseñas ordenado por más recientes."
    )
    @GetMapping("/tutor/{tutorUserId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ReviewResponseDTO>>> getReviewsByTutor(
            @PathVariable Long tutorUserId
    ) {
        List<ReviewResponseDTO> reviews = reviewService.getReviewsByTutor(tutorUserId);
        return ResponseEntity.ok(ApiResponse.success("Reseñas obtenidas", reviews));
    }
}
