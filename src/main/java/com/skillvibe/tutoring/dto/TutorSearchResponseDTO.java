package com.skillvibe.tutoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorSearchResponseDTO {
    private Long id;
    private Long userId;
    private String fullName;
    private String email;
    private String bio;
    private String profilePictureUrl;
    private Double hourlyRate;
    private Integer yearsOfExperience;
    private List<String> subjects;
    private Boolean isVerified;
    /** Promedio de calificaciones del tutor (0.0 - 5.0) */
    private Double averageRating;
    /** Total de reseñas recibidas */
    private Integer totalReviews;
}
