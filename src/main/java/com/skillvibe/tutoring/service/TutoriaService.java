package com.skillvibe.tutoring.service;

import com.skillvibe.tutoring.dto.BookingRequestDTO;
import com.skillvibe.tutoring.model.TutorProfile;
import com.skillvibe.tutoring.model.Tutoria;
import com.skillvibe.tutoring.model.User;
import com.skillvibe.tutoring.repository.TutorProfileRepository;
import com.skillvibe.tutoring.repository.TutoriaRepository;
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

    public TutoriaService(TutoriaRepository tutoriaRepository, UserRepository userRepository, TutorProfileRepository tutorProfileRepository) {
        this.tutoriaRepository = tutoriaRepository;
        this.userRepository = userRepository;
        this.tutorProfileRepository = tutorProfileRepository;
    }

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

        // Descontar saldo
        estudiante.setBalance(estudiante.getBalance() - precio);
        userRepository.save(estudiante);

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

    // 1. Programar y Cobrar al Estudiante
    @SuppressWarnings("null")
    @Transactional
    public Tutoria guardarTutoria(Tutoria tutoria) {
        User estudiante = userRepository.findById(tutoria.getEstudiante().getId())
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));

        if (estudiante.getBalance() < tutoria.getPrecio()) {
            throw new RuntimeException("Saldo insuficiente. ¡Recarga tu cuenta, Andres!");
        }

        // Descontar saldo al alumno
        estudiante.setBalance(estudiante.getBalance() - tutoria.getPrecio());
        userRepository.save(estudiante);

        // Configuración inicial de la clase
        String roomName = "SkillVibe-" + UUID.randomUUID().toString().substring(0, 8);
        tutoria.setMeetingLink("https://meet.jit.si/" + roomName);
        tutoria.setEstado("PROGRAMADA");

        return tutoriaRepository.save(tutoria);
    }

    // 2. BUSCAR ACTIVIDAD (Para el Tablero)
    public List<Tutoria> listarPorUsuario(Long userId) {
        List<Tutoria> comoTutor = tutoriaRepository.findByTutorId(userId);
        List<Tutoria> comoEstudiante = tutoriaRepository.findByEstudianteId(userId);
        comoTutor.addAll(comoEstudiante);
        return comoTutor;
    }

    // 3. ✨ EL MÉTODO QUE FALTABA: FINALIZAR Y PAGAR AL TUTOR
    @SuppressWarnings("null")
    @Transactional
    public Tutoria finalizarTutoria(Long tutoriaId) {
        // Buscamos la tutoría
        Tutoria tutoria = tutoriaRepository.findById(tutoriaId)
                .orElseThrow(() -> new RuntimeException("Tutoría no encontrada con ID: " + tutoriaId));

        // Validamos que no esté ya finalizada
        if ("FINALIZADA".equals(tutoria.getEstado())) {
            throw new RuntimeException("Esta clase ya fue pagada y finalizada.");
        }

        // 1. Cambiamos el estado
        tutoria.setEstado("FINALIZADA");

        // 2. Le sumamos la plata al balance del Tutor
        User tutor = tutoria.getTutor();
        tutor.setBalance(tutor.getBalance() + tutoria.getPrecio());
        userRepository.save(tutor);

        log.info("-----> CLASE FINALIZADA. PAGO REALIZADO AL TUTOR: {}", tutor.getFullName());

        return tutoriaRepository.save(tutoria);
    }
}