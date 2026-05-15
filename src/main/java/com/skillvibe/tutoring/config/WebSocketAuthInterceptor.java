package com.skillvibe.tutoring.config;

import com.skillvibe.tutoring.security.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Interceptor que valida el token JWT en el handshake de WebSocket.
 * Se aplica en el canal de entrada antes de procesar el mensaje STOMP CONNECT.
 */
@Slf4j
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    public WebSocketAuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                if (jwtService.isTokenValid(token)) {
                    String email = jwtService.extractUsername(token);
                    Long userId = jwtService.extractUserId(token);
                    String role = jwtService.extractRole(token);

                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);

                    // Crear principal con los datos del JWT
                    com.skillvibe.tutoring.security.UserPrincipal principal =
                        com.skillvibe.tutoring.security.UserPrincipal.builder()
                            .id(userId)
                            .email(email)
                            .authorities(List.of(authority))
                            .build();

                    UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(principal, null, List.of(authority));

                    accessor.setUser(auth);
                    log.info("WebSocket CONNECT autenticado: userId={}", userId);
                } else {
                    log.warn("WebSocket CONNECT rechazado: token inválido");
                }
            } else {
                log.warn("WebSocket CONNECT sin token Authorization");
            }
        }
        return message;
    }
}
