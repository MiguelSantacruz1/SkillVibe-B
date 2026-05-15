package com.skillvibe.tutoring.service;

import com.skillvibe.tutoring.dto.BookingRequestDTO;
import com.skillvibe.tutoring.model.TutorProfile;
import com.skillvibe.tutoring.model.Transaccion;
import com.skillvibe.tutoring.model.Tutoria;
import com.skillvibe.tutoring.model.User;
import com.skillvibe.tutoring.repository.TutorProfileRepository;
import com.skillvibe.tutoring.repository.TutoriaRepository;
import com.skillvibe.tutoring.repository.TransaccionRepository;
import com.skillvibe.tutoring.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class TutoriaService {

    private final TutoriaRepository tutoriaRepository;
    private final UserRepository userRepository;
    private final TutorProfileRepository tutorProfileRepository;
    private final TransaccionRepository transaccionRepository;

    public TutoriaService(TutoriaRepository tutoriaRepository,
                          UserRepository userRepository,
                          TutorProfileRepository tutorProfileRepository,
                          TransaccionRepository transaccionRepository) {
        this.tutoriaRepository = tutoriaRepository;
        this.userRepository = userRepository;
        this.tutorProfileRepository = tutorProfileRepository;
        this.transaccionRepository = transaccionRepository;
    }

    // ─────────────────────────────────────────────
    // 1. RESERVAR (usado por el endpoint /reservar — STUDENT)
    // ─────────────────────────────────────────────
    @SuppressWarnings("null")
    @Transactional
    public Tutoria reservarTutoria(Long studentId, BookingRequestDTO request) {
        log.info("Procesando reserva para el estudiante: {} con tutor: {}", studentId, request.getTutorId());

        User estudiante = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));

        TutorProfile profile = tutorProfileRepository.findByUserId(request.getTutorId())
                .orElseThrow(() -> new RuntimeException("Perfil de tutor no encontrado"));

        Double precio = profile.getHourlyRate();

        if (estudiante.getBalance() < precio) {
            throw new RuntimeException("Saldo insuficiente para reservar esta clase.");
        }

        // Descontar saldo al estudiante
        estudiante.setBalance(estudiante.getBalance() - precio);
        userRepository.save(estudiante);

        // Registrar transacción de pago para el estudiante
        transaccionRepository.save(Transaccion.builder()
                .user(estudiante)
                .amount(precio)
                .type(Transaccion.TransactionType.PAYMENT)
                .description("Pago por tutoría de " + request.getMateria())
                .build());

        // Crear la tutoría
        Tutoria tutoria = new Tutoria();
        tutoria.setEstudiante(estudiante);
        tutoria.setTutor(profile.getUser());
        tutoria.setMateria(request.getMateria());
        tutoria.setDescripcion(request.getDescripcion());
        tutoria.setFechaHora(request.getFechaHora());
        tutoria.setPrecio(precio);

        String roomName = "SkillVibe-" + UUID.randomUUID().toString().substring(0, 8);
        tutoria.setMeetingLink("https://meet.jit.si/" + roomName);
        tutoria.setEstado("PROGRAMADA");

        return tutoriaRepository.save(tutoria);
    }

    // ─────────────────────────────────────────────
    // 2. PROGRAMAR MANUAL (usado por el endpoint /programar — TUTOR)
    //    Fix #2: ahora también descuenta saldo y registra la transacción
    // ─────────────────────────────────────────────
    @SuppressWarnings("null")
    @Transactional
    public Tutoria guardarTutoria(Tutoria tutoria) {
        User estudiante = userRepository.findById(tutoria.getEstudiante().getId())
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));

        if (estudiante.getBalance() < tutoria.getPrecio()) {
            throw new RuntimeException("Saldo insuficiente. Por favor recarga tu cuenta.");
        }

        // Descontar saldo al alumno
        estudiante.setBalance(estudiante.getBalance() - tutoria.getPrecio());
        userRepository.save(estudiante);

        // Registrar transacción de pago para el alumno (Fix #2: antes faltaba esto)
        transaccionRepository.save(Transaccion.builder()
                .user(estudiante)
                .amount(tutoria.getPrecio())
                .type(Transaccion.TransactionType.PAYMENT)
                .description("Pago por tutoría de " + tutoria.getMateria())
                .build());

        // Configuración inicial de la clase
        String roomName = "SkillVibe-" + UUID.randomUUID().toString().substring(0, 8);
        tutoria.setMeetingLink("https://meet.jit.si/" + roomName);
        tutoria.setEstado("PROGRAMADA");

        return tutoriaRepository.save(tutoria);
    }

    // ─────────────────────────────────────────────
    // 3. LISTAR ACTIVIDAD (Para el Tablero)
    // ─────────────────────────────────────────────
    public List<Tutoria> listarPorUsuario(Long userId) {
        List<Tutoria> comoTutor = tutoriaRepository.findByTutorId(userId);
        List<Tutoria> comoEstudiante = tutoriaRepository.findByEstudianteId(userId);
        comoTutor.addAll(comoEstudiante);
        return comoTutor;
    }

    // ─────────────────────────────────────────────
    // 4. FINALIZAR Y PAGAR AL TUTOR
    //    Fix #3: usa EARNING en vez de REFUND para el ingreso del tutor
    // ─────────────────────────────────────────────
    @SuppressWarnings("null")
    @Transactional
    public Tutoria finalizarTutoria(Long tutoriaId) {
        Tutoria tutoria = tutoriaRepository.findById(tutoriaId)
                .orElseThrow(() -> new RuntimeException("Tutoría no encontrada con ID: " + tutoriaId));

        if ("FINALIZADA".equals(tutoria.getEstado())) {
            throw new RuntimeException("Esta clase ya fue pagada y finalizada.");
        }

        // Cambiar estado
        tutoria.setEstado("FINALIZADA");

        // Sumar el pago al balance del Tutor
        User tutor = tutoria.getTutor();
        tutor.setBalance(tutor.getBalance() + tutoria.getPrecio());
        userRepository.save(tutor);

        // Fix #3: registrar ingreso del tutor con tipo EARNING (no REFUND)
        transaccionRepository.save(Transaccion.builder()
                .user(tutor)
                .amount(tutoria.getPrecio())
                .type(Transaccion.TransactionType.EARNING)
                .description("Ingreso por tutoría finalizada: " + tutoria.getMateria())
                .build());

        log.info("------> CLASE FINALIZADA. PAGO REALIZADO AL TUTOR: {}", tutor.getFullName());

        return tutoriaRepository.save(tutoria);
    }
}