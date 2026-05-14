package com.skillvibe.tutoring.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "transacciones")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Transaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Column(nullable = false)
    private String description;

    private String stripePaymentId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime timestamp;

    public enum TransactionType {
        LOAD,       // Carga de saldo
        PAYMENT,    // Pago por clase
        REFUND,     // Reembolso
        WITHDRAW    // Retiro (para tutores)
    }
}
