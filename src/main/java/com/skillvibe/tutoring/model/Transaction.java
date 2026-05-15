package com.skillvibe.tutoring.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "transactions") // Mantiene la tabla o crea una nueva
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Transaction {

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

    /** ID externo de la transacción en la pasarela de pagos (ej. Wompi transaction ID). */
    @Column(name = "external_payment_id")
    private String externalPaymentId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime timestamp;

    public enum TransactionType {
        LOAD,       // Carga de saldo
        PAYMENT,    // Pago por clase (descuento al estudiante)
        EARNING,    // Ingreso por clase COMPLETED (pago al tutor)
        REFUND,     // Reembolso al estudiante
        WITHDRAW    // Retiro de saldo (para tutores)
    }
}
