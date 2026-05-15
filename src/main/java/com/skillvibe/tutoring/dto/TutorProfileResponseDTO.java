package com.skillvibe.tutoring.dto;

import com.skillvibe.tutoring.model.TutorProfile;
import lombok.Getter;

import java.util.List;

/**
 * DTO seguro para exponer TutorProfile sin exponer la entidad JPA directamente.
 * Evita LazyInitializationException y referencias circulares en Jackson.
 */
@Getter
public class TutorProfileResponseDTO {

    private Long id;
    private Long userId;
    private String fullName;
    private String email;
    private String bio;
    private String profilePictureUrl;
    private String identityCardUrl;
    private String degreeUrl;
    private String credentialsUrl;
    private Double hourlyRate;
    private Integer yearsOfExperience;
    private List<String> subjects;
    private Boolean isVerified;
    private Double averageRating;
    private Integer totalReviews;

    public TutorProfileResponseDTO(TutorProfile profile) {
        this.id = profile.getId();
        this.userId = profile.getUser().getId();
        this.fullName = profile.getUser().getFullName();
        this.email = profile.getUser().getEmail();
        this.bio = profile.getBio();
        this.profilePictureUrl = profile.getProfilePictureUrl();
        this.identityCardUrl = profile.getIdentityCardUrl();
        this.degreeUrl = profile.getDegreeUrl();
        this.credentialsUrl = profile.getCredentialsUrl();
        this.hourlyRate = profile.getHourlyRate();
        this.yearsOfExperience = profile.getYearsOfExperience();
        this.subjects = profile.getSubjects();
        this.isVerified = profile.getIsVerified();
        this.averageRating = profile.getAverageRating();
        this.totalReviews = profile.getTotalReviews();
    }
}
