package com.skillvibe.tutoring.service;

import com.skillvibe.tutoring.dto.BookingRequestDTO;
import com.skillvibe.tutoring.model.TutorProfile;
import com.skillvibe.tutoring.model.TutoringClass;
import com.skillvibe.tutoring.model.User;

import com.skillvibe.tutoring.repository.TutorProfileRepository;
import com.skillvibe.tutoring.repository.TutoringClassRepository;
import com.skillvibe.tutoring.repository.TransactionRepository;
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

    @Mock private TutoringClassRepository tutoringClassRepository;
    @Mock private UserRepository userRepository;
    @Mock private TutorProfileRepository tutorProfileRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;
    @Mock private com.skillvibe.tutoring.service.video.VideoRoomProvider videoRoomProvider;

    @InjectMocks
    private TutoringClassService tutoringClassService;

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
            tutoringClassService.bookClass(student.getId(), request);
        });
        assertEquals("Saldo insuficiente para reservar esta clase.", ex.getMessage());
        verify(tutoringClassRepository, never()).save(any());
    }

    @Test
    void reservarTutoria_withValidData_savesTutoriaAndNotifies() {
        // Arrange
        BookingRequestDTO request = new BookingRequestDTO();
        request.setTutorId(tutorUser.getId());
        request.setSubject("Math");
        request.setScheduledAt(LocalDateTime.now().plusDays(1));

        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(tutorProfileRepository.findByUserId(request.getTutorId())).thenReturn(Optional.of(tutorProfile));
        when(videoRoomProvider.generateMeetingLink(anyString())).thenReturn("https://meet.test");
        when(tutoringClassRepository.save(any(TutoringClass.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TutoringClass result = tutoringClassService.bookClass(student.getId(), request);

        // Assert
        assertNotNull(result);
        assertEquals(com.skillvibe.tutoring.model.ClassStatus.PROGRAMMED, result.getStatus());
        assertEquals(75.0, student.getBalance()); // 100 - 25
        
        verify(userRepository).save(student);
        verify(transactionRepository).save(any());
        verify(eventPublisher).publishEvent(any(com.skillvibe.tutoring.event.ClassReservedEvent.class));
    }

    @Test
    void finalizarTutoria_whenAlreadyFinalizada_throwsException() {
        // Arrange
        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setTutor(tutorUser);
        tutoringClass.setStatus(com.skillvibe.tutoring.model.ClassStatus.COMPLETED);
        when(tutoringClassRepository.findById(1L)).thenReturn(Optional.of(tutoringClass));

        // Act & Assert
        com.skillvibe.tutoring.exception.BusinessLogicException ex = assertThrows(com.skillvibe.tutoring.exception.BusinessLogicException.class, () -> {
            tutoringClassService.finishClass(1L, tutorUser.getId());
        });
        assertEquals("Transición de estado inválida: no se puede pasar de [COMPLETED] a [COMPLETED].", ex.getMessage());
    }
}
