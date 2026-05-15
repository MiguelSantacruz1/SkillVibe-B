package com.skillvibe.tutoring.dto;

import com.skillvibe.tutoring.model.Review;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para una reseña.
 * Expone solo los datos necesarios sin exponer entidades JPA directamente.
 */
@Getter
public class ReviewResponseDTO {

    private Long id;
    private Long tutoriaId;
    private String studentName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;

    public ReviewResponseDTO(Review review) {
        this.id = review.getId();
        this.tutoriaId = review.getTutoria().getId();
        this.studentName = review.getEstudiante().getFullName();
        this.rating = review.getRating();
        this.comment = review.getComment();
        this.createdAt = review.getCreatedAt();
    }
}
