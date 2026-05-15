package com.skillvibe.tutoring.repository;

import com.skillvibe.tutoring.model.TutoringClass;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TutoringClassRepository extends JpaRepository<TutoringClass, Long> {

    // Buscar tutorías donde el usuario sea el TUTOR
    List<TutoringClass> findByTutorId(Long tutorId);

    // Buscar tutorías donde el usuario sea el ESTUDIANTE
    List<TutoringClass> findByEstudianteId(Long estudianteId);
}