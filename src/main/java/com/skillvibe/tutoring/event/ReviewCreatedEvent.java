package com.skillvibe.tutoring.event;

import com.skillvibe.tutoring.model.Review;
import org.springframework.context.ApplicationEvent;

/**
 * Evento de dominio publicado cuando un estudiante crea una nueva reseña.
 * Patrón Observer via Spring ApplicationEvents.
 *
 * ReviewService publica este evento -> NotificationEventListener lo escucha
 * y notifica al tutor sobre la nueva reseña recibida.
 */
public class ReviewCreatedEvent extends ApplicationEvent {

    private final Review review;

    public ReviewCreatedEvent(Object source, Review review) {
        super(source);
        this.review = review;
    }

    public Review getReview() {
        return review;
    }
}
