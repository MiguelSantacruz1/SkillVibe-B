package com.skillvibe.tutoring.service;

import com.skillvibe.tutoring.exception.ResourceNotFoundException;
import com.skillvibe.tutoring.model.StudentProfile;
import com.skillvibe.tutoring.model.User;
import com.skillvibe.tutoring.repository.StudentProfileRepository;
import com.skillvibe.tutoring.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class StudentProfileService {

    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;

    public StudentProfileService(StudentProfileRepository studentProfileRepository,
                                  UserRepository userRepository) {
        this.studentProfileRepository = studentProfileRepository;
        this.userRepository = userRepository;
    }

    /**
     * Obtiene el perfil del estudiante, o crea uno vacío si no existe aún.
     */
    @Transactional
    public StudentProfile getOrCreateProfile(Long userId) {
        return studentProfileRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + userId));
            StudentProfile profile = new StudentProfile();
            profile.setUser(user);
            profile.setInterests(List.of());
            log.info("Creando perfil de estudiante para userId={}", userId);
            return studentProfileRepository.save(profile);
        });
    }

    /**
     * Actualiza bio, foto e intereses del perfil.
     */
    @Transactional
    public StudentProfile updateProfile(Long userId, String bio, String profilePictureUrl, List<String> interests) {
        StudentProfile profile = getOrCreateProfile(userId);
        if (bio != null) profile.setBio(bio);
        if (profilePictureUrl != null) profile.setProfilePictureUrl(profilePictureUrl);
        if (interests != null) profile.setInterests(interests);
        return studentProfileRepository.save(profile);
    }
}
