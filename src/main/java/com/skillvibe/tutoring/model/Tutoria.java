package com.skillvibe.tutoring.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "tutorias")
public class Tutoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String materia;
    private String descripcion;
    private Double precio; // <--- NUEVO: Cuánto cuesta la clase
    private LocalDateTime fechaHora;
    private String meetingLink;

    @ManyToOne
    @JoinColumn(name = "tutor_id")
    private User tutor;

    @ManyToOne
    @JoinColumn(name = "estudiante_id")
    private User estudiante;

    private String estado; // "PROGRAMADA", "EN_CURSO", "FINALIZADA"

    @org.hibernate.annotations.CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @org.hibernate.annotations.UpdateTimestamp
    private LocalDateTime updatedAt;
}
