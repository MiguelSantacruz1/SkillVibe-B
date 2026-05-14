package com.skillvibe.tutoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TutorProfileUpdateDTO {
    private String bio;
    private Double hourlyRate;
    private Integer yearsOfExperience;
    private List<String> subjects;
    private String credentialsUrl;
    private String profilePictureUrl;
}
