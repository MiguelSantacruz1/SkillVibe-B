package com.skillvibe.tutoring.service;

import com.skillvibe.tutoring.dto.NotificationResponseDTO;
import com.skillvibe.tutoring.model.Notification;
import com.skillvibe.tutoring.model.Notification.NotificationType;
import com.skillvibe.tutoring.model.User;
import com.skillvibe.tutoring.repository.NotificationRepository;
import com.skillvibe.tutoring.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de notificaciones en tiempo real.
 *
 * Flujo:
 *  1. Se persiste la notificación en BD.
 *  2. Se envía en tiempo real por WebSocket al canal /user/{userId}/queue/notifications.
 *
 * Si el usuario no está conectado, la notificación se puede recuperar después
 * via GET /api/notifications.
 */
@Slf4j
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(NotificationRepository notificationRepository,
                                UserRepository userRepository,
                                SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Crea, persiste y envía en tiempo real una notificación a un usuario.
     *
     * @param userId  ID del usuario destinatario
     * @param type    Tipo de notificación (BOOKING, REVIEW, etc.)
     * @param message Texto de la notificación
     */
    @SuppressWarnings("null")
    @Transactional
    public void enviarNotificacion(Long userId, NotificationType type, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + userId));

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .message(message)
                .build();

        Notification saved = notificationRepository.save(notification);

        // Enviar en tiempo real por WebSocket al canal personal del usuario
        // El cliente debe suscribirse a /user/queue/notifications
        NotificationResponseDTO dto = new NotificationResponseDTO(saved);
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                dto
        );

        log.info("Notificación enviada a userId={} - tipo={} - msg={}", userId, type, message);
    }

    /**
     * Obtiene las notificaciones no leídas de un usuario (para mostrar al entrar).
     */
    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> getUnreadNotifications(Long userId) {
        return notificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponseDTO::new)
                .toList();
    }

    /**
     * Obtiene el historial completo de notificaciones de un usuario.
     */
    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> getAllNotifications(Long userId) {
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponseDTO::new)
                .toList();
    }

    /**
     * Marca todas las notificaciones de un usuario como leídas.
     */
    @SuppressWarnings("null")
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
        log.info("Notificaciones marcadas como leídas para userId={}", userId);
    }
}
