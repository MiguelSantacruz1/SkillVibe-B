package com.skillvibe.tutoring.service;

import com.skillvibe.tutoring.dto.BookingRequestDTO;
import com.skillvibe.tutoring.event.ClassFinishedEvent;
import com.skillvibe.tutoring.event.ClassReservedEvent;
import com.skillvibe.tutoring.model.TutorProfile;
import com.skillvibe.tutoring.model.Transaction;
import com.skillvibe.tutoring.model.TutoringClass;
import com.skillvibe.tutoring.model.ClassStatus;
import com.skillvibe.tutoring.model.User;
import com.skillvibe.tutoring.repository.TutorProfileRepository;
import com.skillvibe.tutoring.repository.TutoringClassRepository;
import com.skillvibe.tutoring.repository.TransactionRepository;
import com.skillvibe.tutoring.repository.UserRepository;
import com.skillvibe.tutoring.service.video.VideoRoomProvider;
import com.skillvibe.tutoring.exception.BusinessLogicException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de tutorías refactorizado con tres patrones de diseño:
 *
 *  1. Patrón State    → ClassStatus.validarTransicion() controla transiciones válidas.
 *  2. Patrón Observer → ApplicationEventPublisher desacopla las notificaciones.
 *                       TutoringClassService ya no depende de NotificationService.
 *  3. Patrón Strategy → VideoRoomProvider abstrae la generación de links de video.
 *                       Jitsi puede ser reemplazado por Zoom/WebRTC sin cambiar este servicio.
 */
@Slf4j
@Service
public class TutoringClassService {

    private final TutoringClassRepository TutoringClassRepository;
    private final UserRepository userRepository;
    private final TutorProfileRepository tutorProfileRepository;
    private final TransactionRepository TransactionRepository;
    // ── Observer: en vez de NotificationService, usamos el EventPublisher ──
    private final ApplicationEventPublisher eventPublisher;
    // ── Strategy: el VideoRoomProvider es inyectado, no instanciado aquí ──
    private final VideoRoomProvider videoRoomProvider;

    public TutoringClassService(TutoringClassRepository TutoringClassRepository,
                          UserRepository userRepository,
                          TutorProfileRepository tutorProfileRepository,
                          TransactionRepository TransactionRepository,
                          ApplicationEventPublisher eventPublisher,
                          VideoRoomProvider videoRoomProvider) {
        this.TutoringClassRepository = TutoringClassRepository;
        this.userRepository = userRepository;
        this.tutorProfileRepository = tutorProfileRepository;
        this.TransactionRepository = TransactionRepository;
        this.eventPublisher = eventPublisher;
        this.videoRoomProvider = videoRoomProvider;
    }

    // ─────────────────────────────────────────────
    // 1. RESERVAR (usado por el endpoint /reservar — STUDENT)
    // ─────────────────────────────────────────────
    @SuppressWarnings("null")
    @Transactional
    public TutoringClass bookClass(Long studentId, BookingRequestDTO request) {
        log.info("Procesando reserva para el estudiante: {} con tutor: {}", studentId, request.getTutorId());

        User estudiante = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));

        TutorProfile profile = tutorProfileRepository.findByUserId(request.getTutorId())
                .orElseThrow(() -> new RuntimeException("Perfil de tutor no encontrado"));

        Double precio = profile.getHourlyRate();

        if (estudiante.getBalance() < precio) {
            throw new BusinessLogicException("Saldo insuficiente para reservar esta clase.");
        }

        // Descontar saldo al estudiante
        estudiante.setBalance(estudiante.getBalance() - precio);
        userRepository.save(estudiante);

        // Registrar transacción de pago
        TransactionRepository.save(Transaction.builder()
                .user(estudiante)
                .amount(precio)
                .type(Transaction.TransactionType.PAYMENT)
                .description("Pago por tutoría de " + request.getSubject())
                .build());

        // Crear la tutoría con el estado inicial de la máquina de estados
        TutoringClass TutoringClass = new TutoringClass();
        TutoringClass.setStudent(estudiante);
        TutoringClass.setTutor(profile.getUser());
        TutoringClass.setSubject(request.getSubject());
        TutoringClass.setDescription(request.getDescription());
        TutoringClass.setFechaHora(request.getFechaHora());
        TutoringClass.setPrice(precio);
        // ── Strategy: VideoRoomProvider genera el link, sin lógica de Jitsi aquí ──
        TutoringClass.setMeetingLink(videoRoomProvider.generateMeetingLink("reserva-" + studentId));
        // ── State: estado inicial de la máquina de estados ──
        TutoringClass.setStatus(ClassStatus.PROGRAMMED);

        TutoringClass savedTutoria = TutoringClassRepository.save(TutoringClass);

        // ── Observer: publicar evento en vez de llamar a NotificationService ──
        eventPublisher.publishEvent(new ClassReservedEvent(this, savedTutoria));

        return savedTutoria;
    }

    // ─────────────────────────────────────────────
    // 2. PROGRAMAR MANUAL (usado por el endpoint /programar — TUTOR)
    // ─────────────────────────────────────────────
    @SuppressWarnings("null")
    @Transactional
    public TutoringClass saveClass(TutoringClass TutoringClass) {
        User estudiante = userRepository.findById(TutoringClass.getStudent().getId())
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));

        if (estudiante.getBalance() < TutoringClass.getPrice()) {
            throw new BusinessLogicException("Saldo insuficiente. Por favor recarga tu cuenta.");
        }

        // Descontar saldo al alumno
        estudiante.setBalance(estudiante.getBalance() - TutoringClass.getPrice());
        userRepository.save(estudiante);

        // Registrar transacción de pago para el alumno
        TransactionRepository.save(Transaction.builder()
                .user(estudiante)
                .amount(TutoringClass.getPrice())
                .type(Transaction.TransactionType.PAYMENT)
                .description("Pago por tutoría de " + TutoringClass.getSubject())
                .build());

        // ── Strategy: VideoRoomProvider genera el link ──
        TutoringClass.setMeetingLink(videoRoomProvider.generateMeetingLink("manual-" + TutoringClass.getSubject()));
        // ── State: estado inicial de la máquina de estados ──
        TutoringClass.setStatus(ClassStatus.PROGRAMMED);

        TutoringClass savedTutoria = TutoringClassRepository.save(TutoringClass);

        // ── Observer: publicar evento de reserva (el tutor programa, se notifica al estudiante) ──
        eventPublisher.publishEvent(new ClassReservedEvent(this, savedTutoria));

        return savedTutoria;
    }

    // ─────────────────────────────────────────────
    // 3. LISTAR ACTIVIDAD (Para el Tablero)
    // ─────────────────────────────────────────────
    public List<TutoringClass> listByUser(Long userId) {
        List<TutoringClass> comoTutor = TutoringClassRepository.findByTutorId(userId);
        List<TutoringClass> comoEstudiante = TutoringClassRepository.findByStudentId(userId);
        comoTutor.addAll(comoEstudiante);
        return comoTutor;
    }

    // ─────────────────────────────────────────────
    // 4. FINALIZAR Y PAGAR AL TUTOR
    //    ── State: valida que la transición PROGRAMMED/IN_PROGRESS → COMPLETED sea válida ──
    // ─────────────────────────────────────────────
    @SuppressWarnings("null")
    @Transactional
    public TutoringClass finishClass(Long tutoriaId) {
        TutoringClass TutoringClass = TutoringClassRepository.findById(tutoriaId)
                .orElseThrow(() -> new RuntimeException("Tutoría no encontrada con ID: " + tutoriaId));

        // ── State Pattern: validar la transición antes de ejecutar la lógica ──
        TutoringClass.getStatus().validarTransicion(ClassStatus.COMPLETED);

        TutoringClass.setStatus(ClassStatus.COMPLETED);

        // Sumar el pago al balance del Tutor
        User tutor = TutoringClass.getTutor();
        tutor.setBalance(tutor.getBalance() + TutoringClass.getPrice());
        userRepository.save(tutor);

        // Registrar ingreso del tutor con tipo EARNING
        TransactionRepository.save(Transaction.builder()
                .user(tutor)
                .amount(TutoringClass.getPrice())
                .type(Transaction.TransactionType.EARNING)
                .description("Ingreso por tutoría COMPLETED: " + TutoringClass.getSubject())
                .build());

        log.info("-------> CLASE COMPLETED. PAGO REALIZADO AL TUTOR: {}", tutor.getFullName());

        TutoringClass savedTutoria = TutoringClassRepository.save(TutoringClass);

        // ── Observer: publicar evento de finalización ──
        eventPublisher.publishEvent(new ClassFinishedEvent(this, savedTutoria));

        return savedTutoria;
    }

    // ─────────────────────────────────────────────
    // 5. CANCELAR TUTORÍA
    //    Estado nuevo — solo posible desde PROGRAMMED o IN_PROGRESS.
    //    Si ya pagó, se genera REFUND al estudiante.
    // ─────────────────────────────────────────────
    @SuppressWarnings("null")
    @Transactional
    public TutoringClass cancelClass(Long tutoriaId, Long requestingUserId) {
        TutoringClass TutoringClass = TutoringClassRepository.findById(tutoriaId)
                .orElseThrow(() -> new RuntimeException("Tutoría no encontrada con ID: " + tutoriaId));

        // ── State Pattern: validar la transición ──
        TutoringClass.getStatus().validarTransicion(ClassStatus.CANCELLED);
        TutoringClass.setStatus(ClassStatus.CANCELLED);

        // Devolver el dinero al estudiante (REFUND)
        User estudiante = TutoringClass.getStudent();
        estudiante.setBalance(estudiante.getBalance() + TutoringClass.getPrice());
        userRepository.save(estudiante);

        TransactionRepository.save(Transaction.builder()
                .user(estudiante)
                .amount(TutoringClass.getPrice())
                .type(Transaction.TransactionType.REFUND)
                .description("Reembolso por cancelación de clase de " + TutoringClass.getSubject())
                .build());

        log.info("-------> TUTORÍA CANCELLED. REEMBOLSO de ${} AL ESTUDIANTE: {}",
                TutoringClass.getPrice(), estudiante.getFullName());

        return TutoringClassRepository.save(TutoringClass);
    }
}
