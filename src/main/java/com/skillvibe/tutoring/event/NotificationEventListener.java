package com.skillvibe.tutoring.event;

import com.skillvibe.tutoring.model.Notification.NotificationType;
import com.skillvibe.tutoring.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener del Patrón Observer para eventos de dominio relacionados con tutorías y reseñas.
 *
 * Centraliza TODA la lógica de notificaciones en un único lugar.
 * Los servicios de negocio (TutoringClassService, ReviewService) publican eventos sin
 * preocuparse de CÓMO se entregan las notificaciones. Este listener escucha
 * y actúa de forma asíncrona (@Async), mejorando el tiempo de respuesta de los endpoints.
 *
 * Flujo:
 *   TutoringClassService.bookClass()  ──publishes──▶ ClassReservedEvent
 *                                                           │
 *   NotificationEventListener ◀──@EventListener──────────┘
 *                    │
 *                    └──▶ notificationService.enviarNotificacion(...)
 */
@Slf4j
@Component
public class NotificationEventListener {

    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Notifica al TUTOR cuando un estudiante reserva su clase.
     */
    @Async
    @EventListener
    public void onTutoriaReserved(ClassReservedEvent event) {
        var TutoringClass = event.getTutoria();
        log.info("[Observer] ClassReservedEvent recibido para tutoringClassId={}", TutoringClass.getId());

        notificationService.enviarNotificacion(
                TutoringClass.getTutor().getId(),
                NotificationType.BOOKING,
                "El estudiante " + TutoringClass.getStudent().getFullName()
                        + " ha reservado una clase de " + TutoringClass.getSubject() + "."
        );
    }

    /**
     * Notifica al ESTUDIANTE cuando su clase finaliza y puede dejar una reseña.
     */
    @Async
    @EventListener
    public void onTutoriaFinished(ClassFinishedEvent event) {
        var TutoringClass = event.getTutoria();
        log.info("[Observer] ClassFinishedEvent recibido para tutoringClassId={}", TutoringClass.getId());

        notificationService.enviarNotificacion(
                TutoringClass.getStudent().getId(),
                NotificationType.SYSTEM,
                "Tu clase de " + TutoringClass.getSubject() + " con " + TutoringClass.getTutor().getFullName()
                        + " ha finalizado. ¡No olvides dejar una reseña!"
        );
    }

    /**
     * Notifica al TUTOR cuando recibe una nueva reseña.
     */
    @Async
    @EventListener
    public void onReviewCreated(ReviewCreatedEvent event) {
        var review = event.getReview();
        log.info("[Observer] ReviewCreatedEvent recibido para reviewId={}", review.getId());

        notificationService.enviarNotificacion(
                review.getTutor().getId(),
                NotificationType.REVIEW,
                "Has recibido una nueva reseña de " + review.getStudent().getFullName()
                        + " con calificación " + review.getRating() + " estrellas."
        );
    }
}
