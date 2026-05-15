package com.skillvibe.tutoring.event;

import com.skillvibe.tutoring.model.TutoringClass;
import org.springframework.context.ApplicationEvent;

/**
 * Evento de dominio publicado cuando una tutoría es marcada como COMPLETED.
 * Patrón Observer via Spring ApplicationEvents.
 *
 * TutoringClassService publica este evento -> NotificationEventListener lo escucha
 * y notifica al estudiante que puede dejar una reseña.
 */
public class ClassFinishedEvent extends ApplicationEvent {

    private final TutoringClass TutoringClass;

    public ClassFinishedEvent(Object source, TutoringClass TutoringClass) {
        super(source);
        this.TutoringClass = TutoringClass;
    }

    public TutoringClass getTutoria() {
        return TutoringClass;
    }
}
