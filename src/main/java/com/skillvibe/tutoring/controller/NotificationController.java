package com.skillvibe.tutoring.controller;

import com.skillvibe.tutoring.dto.ApiResponse;
import com.skillvibe.tutoring.dto.NotificationResponseDTO;
import com.skillvibe.tutoring.security.UserPrincipal;
import com.skillvibe.tutoring.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notificaciones", description = "Endpoints para obtener y marcar notificaciones como leídas")
@SecurityRequirement(name = "BearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(summary = "Obtener notificaciones no leídas", description = "Devuelve las notificaciones no leídas del usuario autenticado.")
    @GetMapping("/unread")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<NotificationResponseDTO>>> getUnreadNotifications(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        List<NotificationResponseDTO> notifications = notificationService.getUnreadNotifications(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Notificaciones no leídas", notifications));
    }

    @Operation(summary = "Obtener historial completo de notificaciones", description = "Devuelve todas las notificaciones del usuario autenticado.")
    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<NotificationResponseDTO>>> getAllNotifications(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        List<NotificationResponseDTO> notifications = notificationService.getAllNotifications(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Historial de notificaciones", notifications));
    }

    @Operation(summary = "Marcar todas como leídas", description = "Marca todas las notificaciones del usuario como leídas.")
    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        notificationService.markAllAsRead(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Notificaciones marcadas como leídas", null));
    }
}
