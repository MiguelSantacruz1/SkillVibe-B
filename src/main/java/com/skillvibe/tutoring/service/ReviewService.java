package com.skillvibe.tutoring.service;

import com.skillvibe.tutoring.dto.CreateReviewDTO;
import com.skillvibe.tutoring.dto.ReviewResponseDTO;
import com.skillvibe.tutoring.exception.BusinessLogicException;
import com.skillvibe.tutoring.exception.ResourceNotFoundException;
import com.skillvibe.tutoring.model.Review;
import com.skillvibe.tutoring.model.TutorProfile;
import com.skillvibe.tutoring.model.Tutoria;
import com.skillvibe.tutoring.model.User;
import com.skillvibe.tutoring.repository.ReviewRepository;
import com.skillvibe.tutoring.repository.TutorProfileRepository;
import com.skillvibe.tutoring.repository.TutoriaRepository;
import com.skillvibe.tutoring.repository.UserRepository;
import com.skillvibe.tutoring.model.Notification.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final TutoriaRepository tutoriaRepository;
    private final TutorProfileRepository tutorProfileRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public ReviewService(ReviewRepository reviewRepository,
                         TutoriaRepository tutoriaRepository,
                         TutorProfileRepository tutorProfileRepository,
                         UserRepository userRepository,
                         NotificationService notificationService) {
        this.reviewRepository = reviewRepository;
        this.tutoriaRepository = tutoriaRepository;
        this.tutorProfileRepository = tutorProfileRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /**
     * Crea una reseña para una tutoría finalizada.
     * Reglas de negocio:
     *  1. La tutoría debe estar en estado FINALIZADA.
     *  2. El estudiante debe ser el mismo que cursó la tutoría.
     *  3. Solo se permite 1 reseña por tutoría.
     * Al finalizar, recalcula el averageRating del tutor de forma atómica.
     */
    @SuppressWarnings("null")
    @Transactional
    public ReviewResponseDTO crearReview(Long studentId, CreateReviewDTO dto) {
        Tutoria tutoria = tutoriaRepository.findById(dto.getTutoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Tutoría no encontrada con ID: " + dto.getTutoriaId()));

        // Regla 1: la tutoría debe estar finalizada
        if (!"FINALIZADA".equals(tutoria.getEstado())) {
            throw new BusinessLogicException("Solo puedes calificar tutorías que hayan finalizado.");
        }

        // Regla 2: el estudiante que califica debe ser el que tomó la clase
        if (!tutoria.getEstudiante().getId().equals(studentId)) {
            throw new BusinessLogicException("Solo el estudiante de esta tutoría puede calificarla.");
        }

        // Regla 3: no se puede calificar dos veces la misma tutoría
        if (reviewRepository.existsByTutoriaId(dto.getTutoriaId())) {
            throw new BusinessLogicException("Ya existe una reseña para esta tutoría.");
        }

        User estudiante = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado"));

        Review review = Review.builder()
                .tutor(tutoria.getTutor())
                .estudiante(estudiante)
                .tutoria(tutoria)
                .rating(dto.getRating())
                .comment(dto.getComment())
                .build();

        Review savedReview = reviewRepository.save(review);

        // Recalcular el rating promedio del tutor de forma atómica
        recalcularRatingTutor(tutoria.getTutor().getId());

        // Notificar al tutor sobre la nueva reseña
        notificationService.enviarNotificacion(
                tutoria.getTutor().getId(),
                NotificationType.REVIEW,
                "Has recibido una nueva reseña de " + estudiante.getFullName() + " con calificación " + dto.getRating() + " estrellas."
        );

        log.info("Reseña creada para la tutoría {} con rating {}", dto.getTutoriaId(), dto.getRating());
        return new ReviewResponseDTO(savedReview);
    }

    /**
     * Obtiene todas las reseñas públicas de un tutor, ordenadas por más recientes.
     */
    @SuppressWarnings("null")
    @Transactional(readOnly = true)
    public List<ReviewResponseDTO> getReviewsByTutor(Long tutorId) {
        return reviewRepository.findByTutorIdOrderByCreatedAtDesc(tutorId)
                .stream()
                .map(ReviewResponseDTO::new)
                .toList();
    }

    /**
     * Recalcula y persiste el promedio de rating y total de reseñas del tutor.
     * Se llama dentro de la misma transacción para garantizar consistencia.
     */
    private void recalcularRatingTutor(Long tutorUserId) {
        TutorProfile profile = tutorProfileRepository.findByUserId(tutorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de tutor no encontrado"));

        double avg = reviewRepository.findAverageRatingByTutorId(tutorUserId).orElse(0.0);
        long total = reviewRepository.countByTutorId(tutorUserId);

        // Redondear a 1 decimal
        profile.setAverageRating(Math.round(avg * 10.0) / 10.0);
        profile.setTotalReviews((int) total);
        tutorProfileRepository.save(profile);

        log.info("Rating del tutor (userId={}) actualizado: {} ({} reseñas)", tutorUserId, profile.getAverageRating(), total);
    }
}
