package com.skillvibe.tutoring.service;

import com.skillvibe.tutoring.dto.BookingRequestDTO;
import com.skillvibe.tutoring.model.TutorProfile;
import com.skillvibe.tutoring.model.Tutoria;
import com.skillvibe.tutoring.model.User;
import com.skillvibe.tutoring.model.Notification.NotificationType;
import com.skillvibe.tutoring.repository.TutorProfileRepository;
import com.skillvibe.tutoring.repository.TutoriaRepository;
import com.skillvibe.tutoring.repository.TransaccionRepository;
import com.skillvibe.tutoring.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class TutoriaServiceTest {

    @Mock private TutoriaRepository tutoriaRepository;
    @Mock private UserRepository userRepository;
    @Mock private TutorProfileRepository tutorProfileRepository;
    @Mock private TransaccionRepository transaccionRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private TutoriaService tutoriaService;

    private User student;
    private User tutorUser;
    private TutorProfile tutorProfile;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(1L);
        student.setBalance(100.0);
        student.setFullName("Student User");

        tutorUser = new User();
        tutorUser.setId(2L);
        tutorUser.setBalance(0.0);
        tutorUser.setFullName("Tutor User");

        tutorProfile = new TutorProfile();
        tutorProfile.setId(1L);
        tutorProfile.setUser(tutorUser);
        tutorProfile.setHourlyRate(25.0);
    }

    @Test
    void reservarTutoria_withInsufficientBalance_throwsException() {
        // Arrange
        student.setBalance(10.0); // Less than hourly rate (25.0)
        BookingRequestDTO request = new BookingRequestDTO();
        request.setTutorId(tutorUser.getId());
        
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(tutorProfileRepository.findByUserId(request.getTutorId())).thenReturn(Optional.of(tutorProfile));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            tutoriaService.reservarTutoria(student.getId(), request);
        });
        assertEquals("Saldo insuficiente para reservar esta clase.", ex.getMessage());
        verify(tutoriaRepository, never()).save(any());
    }

    @Test
    void reservarTutoria_withValidData_savesTutoriaAndNotifies() {
        // Arrange
        BookingRequestDTO request = new BookingRequestDTO();
        request.setTutorId(tutorUser.getId());
        request.setMateria("Math");
        request.setFechaHora(LocalDateTime.now().plusDays(1));

        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(tutorProfileRepository.findByUserId(request.getTutorId())).thenReturn(Optional.of(tutorProfile));
        when(tutoriaRepository.save(any(Tutoria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Tutoria result = tutoriaService.reservarTutoria(student.getId(), request);

        // Assert
        assertNotNull(result);
        assertEquals("PROGRAMADA", result.getEstado());
        assertEquals(75.0, student.getBalance()); // 100 - 25
        
        verify(userRepository).save(student);
        verify(transaccionRepository).save(any());
        verify(notificationService).enviarNotificacion(eq(tutorUser.getId()), eq(NotificationType.BOOKING), anyString());
    }

    @Test
    void finalizarTutoria_whenAlreadyFinalizada_throwsException() {
        // Arrange
        Tutoria tutoria = new Tutoria();
        tutoria.setEstado("FINALIZADA");
        when(tutoriaRepository.findById(1L)).thenReturn(Optional.of(tutoria));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            tutoriaService.finalizarTutoria(1L);
        });
        assertEquals("Esta clase ya fue pagada y finalizada.", ex.getMessage());
    }
}
