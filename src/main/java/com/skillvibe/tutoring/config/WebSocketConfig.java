package com.skillvibe.tutoring.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuración del broker STOMP sobre WebSocket.
 *
 * Protocolo de comunicación:
 *  - Cliente se conecta a:  /ws  (con fallback SockJS)
 *  - Suscripción personal:  /queue/notifications  (notificaciones del usuario autenticado)
 *  - Envío desde cliente:   /app/...  (si se necesita en el futuro)
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor authInterceptor;

    public WebSocketConfig(WebSocketAuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Broker en memoria para colas personales (/queue) y topics (/topic)
        registry.enableSimpleBroker("/queue", "/topic");
        // Prefijo para mensajes enviados desde el cliente al servidor
        registry.setApplicationDestinationPrefixes("/app");
        // Prefijo para mensajes dirigidos a un usuario específico
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // CORS controlado por SecurityConfig
                .withSockJS();                  // Fallback para navegadores que no soportan WebSocket nativo
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Registrar el interceptor JWT en el canal de entrada
        registration.interceptors(authInterceptor);
    }
}
