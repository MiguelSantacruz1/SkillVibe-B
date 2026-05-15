package com.skillvibe.tutoring.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Notificación persistida en base de datos para un usuario.
 * Se envía también en tiempo real por WebSocket al canal /queue/notifications.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Usuario destinatario de la notificación */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(nullable = false)
    private String message;

    /** false = no leída, true = leída */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public enum NotificationType {
        BOOKING,    // Nueva reserva de clase
        REVIEW,     // Nueva reseña recibida
        VERIFIED,   // Tutor verificado por admin
        REJECTED,   // Tutor rechazado por admin
        SYSTEM      // Mensaje del sistema
    }
}
