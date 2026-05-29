package com.skillvibe.tutoring.service.video;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Implementación concreta del Patrón Strategy para Jitsi Meet.
 *
 * Genera un link único de Jitsi para cada sesión. Si en el futuro
 * SkillVibe migra a Zoom o a su propio servidor WebRTC (Smart Edu Board),
 * solo se necesita crear una nueva implementación de VideoRoomProvider
 * e inyectarla, sin modificar TutoringClassService.
 *
 * Naming convention de la sala: "SV-{sessionId}-{uniqueId}"
 * Ejemplo: SV-TutoringClass-abc1234
 */
@Slf4j
@Component
public class JitsiVideoRoomProvider implements VideoRoomProvider {

    private static final String VERCEL_BASE_URL = "https://frontend-intelligent-table-dp2tk3c8z.vercel.app/";
    private static final String ROOM_PREFIX = "room-";

    @Override
    public String generateMeetingLink(String sessionId) {
        String uniquePart = UUID.randomUUID().toString().substring(0, 8);
        String roomName = ROOM_PREFIX + uniquePart;
        String meetingLink = VERCEL_BASE_URL + roomName;
        log.info("VideoRoom generado para sessionId [{}]: {}", sessionId, meetingLink);
        return meetingLink;
    }
}
