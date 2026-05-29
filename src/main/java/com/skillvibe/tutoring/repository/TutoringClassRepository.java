package com.skillvibe.tutoring.repository;

import com.skillvibe.tutoring.model.TutoringClass;
import com.skillvibe.tutoring.model.ClassStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface TutoringClassRepository extends JpaRepository<TutoringClass, Long> {

    // Buscar tutorías donde el usuario sea el TUTOR
    List<TutoringClass> findByTutorId(Long tutorId);

    // Buscar tutorías donde el usuario sea el ESTUDIANTE
    List<TutoringClass> findByStudentId(Long studentId);

    // Métodos paginados para el Dashboard
    Page<TutoringClass> findByTutorId(Long tutorId, Pageable pageable);
    Page<TutoringClass> findByStudentId(Long studentId, Pageable pageable);

    // Para el scheduler de recordatorios: clases PROGRAMMED que empiezan entre from y to
    List<TutoringClass> findByStatusAndScheduledAtBetween(
            ClassStatus status, LocalDateTime from, LocalDateTime to);
}