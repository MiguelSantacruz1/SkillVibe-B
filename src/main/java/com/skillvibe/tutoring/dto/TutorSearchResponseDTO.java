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
}
