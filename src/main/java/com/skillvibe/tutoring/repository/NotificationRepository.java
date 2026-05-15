package com.skillvibe.tutoring.repository;

import com.skillvibe.tutoring.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Notificaciones no leídas de un usuario, más recientes primero */
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    /** Todas las notificaciones de un usuario (historial completo) */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Marcar todas las notificaciones de un usuario como leídas */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    void markAllAsReadByUserId(@Param("userId") Long userId);
}
