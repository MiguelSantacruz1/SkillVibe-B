package com.skillvibe.tutoring.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO para crear una calificación y reseña sobre una tutoría COMPLETED.
 */
@Data
public class CreateReviewDTO {

    @NotNull(message = "El ID de la tutoría es obligatorio")
    private Long tutoriaId;

    @NotNull(message = "La calificación es obligatoria")
    @Min(value = 1, message = "La calificación mínima es 1")
    @Max(value = 5, message = "La calificación máxima es 5")
    private Integer rating;

    @Size(max = 500, message = "El comentario no puede exceder 500 caracteres")
    private String comment;
}
