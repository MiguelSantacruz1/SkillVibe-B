package com.skillvibe.tutoring.service.video;

/**
 * Interfaz del Patrón Strategy para proveedores de salas de video.
 *
 * Define el contrato para generar links de videollamada. TutoringClassService
 * depende de esta abstracción, no de Jitsi directamente, lo que permite
 * intercambiar el proveedor (Jitsi → Zoom, WebRTC propio) sin modificar
 * la lógica de negocio de reservas.
 */
public interface VideoRoomProvider {

    /**
     * Genera un enlace único para una sala de videollamada.
     *
     * @param sessionId Identificador base para el nombre de la sala (ej. ID de tutoría).
     * @return URL completa para unirse a la sala de video.
     */
    String generateMeetingLink(String sessionId);
}
