package com.skillvibe.tutoring.repository;

import com.skillvibe.tutoring.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /** Todas las reseñas de un tutor, ordenadas por fecha descendente */
    List<Review> findByTutorIdOrderByCreatedAtDesc(Long tutorId);

    /** Buscar reseña específica de un estudiante sobre una tutoría (para evitar duplicados) */
    Optional<Review> findByEstudianteIdAndTutoriaId(Long estudianteId, Long tutoriaId);

    /** Verifica si ya existe una reseña para esa tutoría */
    boolean existsByTutoriaId(Long tutoriaId);

    /** Promedio de rating de un tutor (para recálculo) */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.tutor.id = :tutorId")
    Optional<Double> findAverageRatingByTutorId(@Param("tutorId") Long tutorId);

    /** Total de reseñas de un tutor */
    long countByTutorId(Long tutorId);
}
