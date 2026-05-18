package com.skillvibe.tutoring.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @JsonProperty("fullName")
    private String fullName;

    @Column(unique = true, nullable = false)
    @JsonProperty("email")
    private String email;

    @Column(nullable = false)
    @JsonProperty("password")
    private String password;

    @Enumerated(EnumType.STRING)
    @JsonProperty("role")
    private Role role;

    @JsonProperty("balance")
    private Double balance = 0.0;

    // ── Verificación de correo ────────────────────────────────────────────────
    @Column(nullable = false)
    private Boolean emailVerified = false;

    @Column(length = 64)
    private String verificationToken;

    private java.time.LocalDateTime verificationTokenExpiry;

    // ── Recuperación de contraseña ───────────────────────────────────────────
    @Column(length = 64)
    private String passwordResetToken;

    private java.time.LocalDateTime passwordResetTokenExpiry;

    // ── Auditoría ────────────────────────────────────────────────────────────
    @org.hibernate.annotations.CreationTimestamp
    @Column(updatable = false)
    private java.time.LocalDateTime createdAt;

    @org.hibernate.annotations.UpdateTimestamp
    private java.time.LocalDateTime updatedAt;
}