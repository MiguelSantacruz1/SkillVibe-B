package com.skillvibe.tutoring.security;

import com.skillvibe.tutoring.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    // Leída desde application.properties o variable de entorno JWT_SECRET
    @Value("${app.jwt.secret}")
    private String secretString;

    @Value("${app.jwt.expiration:86400000}")
    private long EXPIRATION_TIME;

    private SecretKey SECRET_KEY;

    @PostConstruct
    public void init() {
        if (secretString == null || secretString.trim().isEmpty() || secretString.length() < 32) {
            throw new IllegalStateException(
                "CRITICAL: La variable de entorno JWT_SECRET no está configurada o es demasiado corta (mínimo 32 caracteres). " +
                "La aplicación no puede arrancar de forma segura sin ella. Configúrala en Railway."
            );
        }
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = md.digest(secretString.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            this.SECRET_KEY = Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error initializing JWT secret key", e);
        }
    }

    // CAMBIO: Ahora recibe el objeto User completo para sacarle el Rol
    public String generateToken(User user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())       // <-- Guardamos el ID
                .claim("role", user.getRole().name()) // <-- Guardamos el ROL
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SECRET_KEY)
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(SECRET_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    // ESTE ES EL MÉTODO QUE TE FALTABA
    public String extractRole(String token) {
        return Jwts.parser()
                .verifyWith(SECRET_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }

    public Long extractUserId(String token) {
        return Jwts.parser()
                .verifyWith(SECRET_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("userId", Long.class);
    }

    public boolean isTokenValid(String token) {
        try {
            Jwts.parser().verifyWith(SECRET_KEY).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}