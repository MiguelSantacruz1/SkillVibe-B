package com.skillvibe.tutoring.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "tutoringClasses")
public class TutoringClass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String subject;
    private String description;
    private Double price; // <--- NUEVO: Cuánto cuesta la clase
    private LocalDateTime scheduledAt;
    private String meetingLink;

    @ManyToOne
    @JoinColumn(name = "tutor_id")
    private User tutor;

    @ManyToOne
    @JoinColumn(name = "estudiante_id")
    private User student;

    @Enumerated(EnumType.STRING)
    private ClassStatus status; // PROGRAMMED → IN_PROGRESS → COMPLETED | CANCELLED

    @org.hibernate.annotations.CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @org.hibernate.annotations.UpdateTimestamp
    private LocalDateTime updatedAt;
}
