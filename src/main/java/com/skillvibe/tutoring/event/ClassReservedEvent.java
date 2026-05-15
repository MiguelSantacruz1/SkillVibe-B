package com.skillvibe.tutoring.event;

import com.skillvibe.tutoring.model.TutoringClass;
import org.springframework.context.ApplicationEvent;

/**
 * Evento de dominio publicado cuando un estudiante reserva una tutoría.
 * Patrón Observer via Spring ApplicationEvents.
 *
 * TutoringClassService publica este evento -> NotificationEventListener lo escucha
 * y envía la notificación. Así TutoringClassService NO depende de NotificationService.
 */
public class ClassReservedEvent extends ApplicationEvent {

    private final TutoringClass TutoringClass;

    public ClassReservedEvent(Object source, TutoringClass TutoringClass) {
        super(source);
        this.TutoringClass = TutoringClass;
    }

    public TutoringClass getTutoria() {
        return TutoringClass;
    }
}
