package com.skillvibe.tutoring.service;

import com.skillvibe.tutoring.dto.AdminVerifyDTO;
import com.skillvibe.tutoring.dto.TutorProfileResponseDTO;
import com.skillvibe.tutoring.exception.ResourceNotFoundException;
import com.skillvibe.tutoring.model.Notification.NotificationType;
import com.skillvibe.tutoring.model.TutorProfile;
import com.skillvibe.tutoring.repository.TutorProfileRepository;
import com.skillvibe.tutoring.repository.TutoringClassRepository;
import com.skillvibe.tutoring.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AdminService {

    private final TutorProfileRepository tutorProfileRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final TutoringClassRepository TutoringClassRepository;

    public AdminService(TutorProfileRepository tutorProfileRepository,
                        NotificationService notificationService,
                        UserRepository userRepository,
                        TutoringClassRepository TutoringClassRepository) {
        this.tutorProfileRepository = tutorProfileRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.TutoringClassRepository = TutoringClassRepository;
    }

    @Transactional(readOnly = true)
    public List<TutorProfileResponseDTO> getPendingTutors() {
        return tutorProfileRepository.findAll().stream()
                .filter(profile -> !profile.getIsVerified())
                .map(TutorProfileResponseDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TutorProfileResponseDTO> getVerifiedTutors() {
        return tutorProfileRepository.findAll().stream()
                .filter(TutorProfile::getIsVerified)
                .map(TutorProfileResponseDTO::new)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("null")
    @Transactional
    public TutorProfileResponseDTO verifyTutor(Long tutorProfileId, AdminVerifyDTO dto) {
        TutorProfile profile = tutorProfileRepository.findById(tutorProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de tutor no encontrado con ID: " + tutorProfileId));

        profile.setIsVerified(dto.getVerified());
        TutorProfile savedProfile = tutorProfileRepository.save(profile);

        // Notificar al tutor
        NotificationType type = dto.getVerified() ? NotificationType.VERIFIED : NotificationType.REJECTED;
        String message = dto.getVerified() 
                ? "¡Felicidades! Tu perfil ha sido verificado por un administrador. Ya puedes aparecer en las búsquedas."
                : "Tu solicitud de verificación ha sido rechazada. Motivo: " + (dto.getReason() != null ? dto.getReason() : "No especificado");

        notificationService.enviarNotificacion(
                profile.getUser().getId(),
                type,
                message
        );

        log.info("Tutor {} (ID: {}) verificación actualizada a {}", profile.getUser().getFullName(), profile.getId(), dto.getVerified());
        
        return new TutorProfileResponseDTO(savedProfile);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalTutors", tutorProfileRepository.count());
        stats.put("totalTutorias", TutoringClassRepository.count());
        return stats;
    }
}
