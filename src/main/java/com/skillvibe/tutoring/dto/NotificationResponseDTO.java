package com.skillvibe.tutoring.dto;

import com.skillvibe.tutoring.model.Notification;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para notificaciones.
 * Enviado tanto por REST (/api/notifications) como por WebSocket (/queue/notifications).
 */
@Getter
public class NotificationResponseDTO {

    private Long id;
    private String type;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public NotificationResponseDTO(Notification notification) {
        this.id = notification.getId();
        this.type = notification.getType().name();
        this.message = notification.getMessage();
        this.isRead = notification.getIsRead();
        this.createdAt = notification.getCreatedAt();
    }
}
