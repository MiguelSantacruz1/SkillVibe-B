package com.skillvibe.tutoring.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "tutor_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TutorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String profilePictureUrl;
    
    @Column(nullable = false)
    private String identityCardUrl;
    
    @Column(nullable = false)
    private String degreeUrl;

    private String credentialsUrl;

    @Column(nullable = false)
    private Double hourlyRate;

    private Integer yearsOfExperience;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tutor_subjects", joinColumns = @JoinColumn(name = "tutor_profile_id"))
    @Column(name = "subject")
    private List<String> subjects;

    @Column(nullable = false)
    private Boolean isVerified = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
