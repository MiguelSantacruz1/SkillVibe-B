package com.skillvibe.tutoring.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Representa la calificación y reseña que un estudiante deja
 * sobre una tutoría finalizada. Solo se permite 1 reseña por tutoría.
 */
@Entity
@Table(
    name = "reviews",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_review_tutoria",
        columnNames = {"tutoria_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** El tutor que recibe la calificación */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    private User tutor;

    /** El estudiante que deja la calificación */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private User estudiante;

    /** La tutoría sobre la que se hace la reseña (1:1, única) */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutoria_id", nullable = false)
    private Tutoria tutoria;

    /** Calificación del 1 al 5 */
    @Column(nullable = false)
    @Min(1)
    @Max(5)
    private Integer rating;

    /** Comentario opcional del estudiante */
    @Column(columnDefinition = "TEXT")
    private String comment;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
