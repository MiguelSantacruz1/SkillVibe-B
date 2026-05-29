package com.skillvibe.tutoring.service;

import com.skillvibe.tutoring.dto.TutorSearchResponseDTO;
import com.skillvibe.tutoring.model.TutorProfile;
import com.skillvibe.tutoring.model.TutoringClass;
import com.skillvibe.tutoring.repository.TutorProfileRepository;
import com.skillvibe.tutoring.repository.TutoringClassRepository;
import com.skillvibe.tutoring.specification.TutorProfileSpecification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class TutorService {

    private final TutoringClassRepository TutoringClassRepository;
    private final TutorProfileRepository tutorProfileRepository;

    public TutorService(TutoringClassRepository TutoringClassRepository, TutorProfileRepository tutorProfileRepository) {
        this.TutoringClassRepository = TutoringClassRepository;
        this.tutorProfileRepository = tutorProfileRepository;
    }

    @SuppressWarnings("null")
    public Page<TutorSearchResponseDTO> searchTutors(
            String query,
            String subject,
            Double minPrice,
            Double maxPrice,
            Integer minExperience,
            Boolean onlyVerified,
            Pageable pageable
    ) {
        log.info("Buscando tutores con filtros - Query: {}, Subject: {}", query, subject);
        
        Specification<TutorProfile> spec = TutorProfileSpecification.filterByCriteria(
                query, subject, minPrice, maxPrice, minExperience, onlyVerified
        );

        return tutorProfileRepository.findAll(spec, pageable)
                .map(this::convertToSearchDTO);
    }

    private TutorSearchResponseDTO convertToSearchDTO(TutorProfile profile) {
        return TutorSearchResponseDTO.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .fullName(profile.getUser().getFullName())
                .email(profile.getUser().getEmail())
                .bio(profile.getBio())
                .profilePictureUrl(profile.getProfilePictureUrl())
                .hourlyRate(profile.getHourlyRate())
                .yearsOfExperience(profile.getYearsOfExperience())
                .subjects(profile.getSubjects())
                .isVerified(profile.getIsVerified())
                .averageRating(profile.getAverageRating())
                .totalReviews(profile.getTotalReviews())
                .build();
    }

    public List<TutoringClass> getTutoringClassesByTutor(Long tutorId) {
        log.info("Obteniendo panel de tutorías para el tutor: {}", tutorId);
        return TutoringClassRepository.findByTutorId(tutorId);
    }

    public TutorProfile getProfileByUserId(Long userId) {
        return tutorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Perfil de tutor no encontrado"));
    }

    public TutorProfile updateProfile(Long userId, com.skillvibe.tutoring.dto.TutorProfileUpdateDTO updateDTO) {
        TutorProfile profile = getProfileByUserId(userId);
        
        profile.setBio(updateDTO.getBio());
        profile.setHourlyRate(updateDTO.getHourlyRate());
        profile.setYearsOfExperience(updateDTO.getYearsOfExperience());
        profile.setSubjects(updateDTO.getSubjects());
        profile.setCredentialsUrl(updateDTO.getCredentialsUrl());
        profile.setProfilePictureUrl(updateDTO.getProfilePictureUrl());
        
        return tutorProfileRepository.save(profile);
    }

    public String getTutorPanelMessage() {
        return "Bienvenido al panel de profesores de SkillVibe.";
    }
}
