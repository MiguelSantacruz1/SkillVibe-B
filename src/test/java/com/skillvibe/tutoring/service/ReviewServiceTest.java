package com.skillvibe.tutoring.service;

import com.skillvibe.tutoring.dto.CreateReviewDTO;
import com.skillvibe.tutoring.dto.ReviewResponseDTO;
import com.skillvibe.tutoring.exception.BusinessLogicException;
import com.skillvibe.tutoring.model.Review;
import com.skillvibe.tutoring.model.TutorProfile;
import com.skillvibe.tutoring.model.TutoringClass;
import com.skillvibe.tutoring.model.User;

import com.skillvibe.tutoring.repository.ReviewRepository;
import com.skillvibe.tutoring.repository.TutorProfileRepository;
import com.skillvibe.tutoring.repository.TutoringClassRepository;
import com.skillvibe.tutoring.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private TutoringClassRepository tutoringClassRepository;
    @Mock private TutorProfileRepository tutorProfileRepository;
    @Mock private UserRepository userRepository;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ReviewService reviewService;

    private User student;
    private User tutorUser;
    private TutoringClass tutoringClass;
    private TutorProfile tutorProfile;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(1L);

        tutorUser = new User();
        tutorUser.setId(2L);

        tutoringClass = new TutoringClass();
        tutoringClass.setId(10L);
        tutoringClass.setStudent(student);
        tutoringClass.setTutor(tutorUser);
        
        tutorProfile = new TutorProfile();
        tutorProfile.setId(1L);
        tutorProfile.setUser(tutorUser);
    }

    @Test
    void crearReview_whenTutoriaNotFinalizada_throwsException() {
        // Arrange
        tutoringClass.setStatus(com.skillvibe.tutoring.model.ClassStatus.PROGRAMMED);
        CreateReviewDTO dto = new CreateReviewDTO();
        dto.setTutoriaId(10L);

        when(tutoringClassRepository.findById(10L)).thenReturn(Optional.of(tutoringClass));

        // Act & Assert
        BusinessLogicException ex = assertThrows(BusinessLogicException.class, () -> {
            reviewService.crearReview(1L, dto);
        });
        assertEquals("Solo puedes calificar tutorías que hayan finalizado.", ex.getMessage());
    }

    @Test
    void crearReview_whenDuplicate_throwsException() {
        // Arrange
        tutoringClass.setStatus(com.skillvibe.tutoring.model.ClassStatus.COMPLETED);
        CreateReviewDTO dto = new CreateReviewDTO();
        dto.setTutoriaId(10L);

        when(tutoringClassRepository.findById(10L)).thenReturn(Optional.of(tutoringClass));
        when(reviewRepository.existsByTutoriaId(10L)).thenReturn(true);

        // Act & Assert
        BusinessLogicException ex = assertThrows(BusinessLogicException.class, () -> {
            reviewService.crearReview(1L, dto);
        });
        assertEquals("Ya existe una reseña para esta tutoría.", ex.getMessage());
    }

    @Test
    void crearReview_updatesAverageRating() {
        // Arrange
        tutoringClass.setStatus(com.skillvibe.tutoring.model.ClassStatus.COMPLETED);
        CreateReviewDTO dto = new CreateReviewDTO();
        dto.setTutoriaId(10L);
        dto.setRating(5);
        dto.setComment("Great!");

        when(tutoringClassRepository.findById(10L)).thenReturn(Optional.of(tutoringClass));
        when(reviewRepository.existsByTutoriaId(10L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        
        Review savedReview = new Review();
        savedReview.setId(100L);
        savedReview.setRating(5);
        savedReview.setComment("Great!");
        savedReview.setStudent(student);
        savedReview.setTutor(tutorUser);
        savedReview.setTutoringClass(tutoringClass);
        
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);
        
        when(tutorProfileRepository.findByUserId(tutorUser.getId())).thenReturn(Optional.of(tutorProfile));
        when(reviewRepository.findAverageRatingByTutorId(tutorUser.getId())).thenReturn(Optional.of(4.5));
        when(reviewRepository.countByTutorId(tutorUser.getId())).thenReturn(2L);

        // Act
        ReviewResponseDTO result = reviewService.crearReview(1L, dto);

        // Assert
        assertNotNull(result);
        assertEquals(5, result.getRating());
        
        verify(tutorProfileRepository).save(tutorProfile);
        assertEquals(4.5, tutorProfile.getAverageRating());
        assertEquals(2, tutorProfile.getTotalReviews());
        
        verify(eventPublisher).publishEvent(any(com.skillvibe.tutoring.event.ReviewCreatedEvent.class));
    }
}
