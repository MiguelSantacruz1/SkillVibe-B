package com.skillvibe.tutoring.service;

import com.skillvibe.tutoring.model.Transaction;
import com.skillvibe.tutoring.model.User;
import com.skillvibe.tutoring.repository.TransactionRepository;
import com.skillvibe.tutoring.repository.UserRepository;
import com.skillvibe.tutoring.service.payment.PaymentProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de pagos refactorizado con el Patrón Strategy.
 *
 * PaymentService ya NO conoce la SDK de Stripe. En su lugar, depende de
 * la interfaz PaymentProcessor, que actualmente tiene como implementación
 * StripePaymentProcessor. Para agregar PayPal, solo se necesita:
 *   1. Crear PayPalPaymentProcessor implements PaymentProcessor.
 *   2. Cambiar la inyección en el contexto de Spring (ej. @Qualifier).
 */
@Slf4j
@Service
public class PaymentService {

    // ── Strategy: dependencia invertida → interface, no implementación concreta ──
    private final PaymentProcessor paymentProcessor;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public PaymentService(PaymentProcessor paymentProcessor,
                       TransactionRepository transactionRepository,
                       UserRepository userRepository) {
        this.paymentProcessor = paymentProcessor;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    /**
     * Crea una sesión de pago delegando al PaymentProcessor activo (Stripe).
     *
     * @param user   Usuario que realiza la recarga.
     * @param amount Monto en USD.
     * @return URL de la sesión de checkout.
     */
    public String createCheckoutSession(User user, Double amount) throws Exception {
        log.info("Iniciando checkout para usuario: {} por ${}", user.getEmail(), amount);
        // ── Strategy: llamada al procesador de pago activo ──
        return paymentProcessor.createCheckoutSession(user, amount);
    }

    /**
     * Procesa un pago exitoso (llamado por el webhook / endpoint de éxito).
     * Acredita el saldo al usuario y registra la transacción.
     *
     * NOTA: Pendiente de implementar validación de firma de Stripe Webhook
     * para prevenir pagos simulados fraudulentamente.
     */
    @SuppressWarnings("null")
    @Transactional
    public void processSuccessfulPayment(String userId, String amount, String paymentId) {
        Long uId = Long.parseLong(userId);
        Double amt = Double.parseDouble(amount);

        User user = userRepository.findById(uId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Actualizar balance
        user.setBalance(user.getBalance() + amt);
        userRepository.save(user);

        // Registrar transacción de recarga
        Transaction transaction = Transaction.builder()
                .user(user)
                .amount(amt)
                .type(Transaction.TransactionType.LOAD)
                .description("Recarga de saldo vía Stripe")
                .stripePaymentId(paymentId)
                .build();

        transactionRepository.save(transaction);
        log.info("Pago procesado con éxito para el usuario {}: +${}", user.getFullName(), amt);
    }

    /**
     * Devuelve el historial de Transactiones de un usuario.
     */
    public List<Transaction> getHistory(Long userId) {
        return transactionRepository.findByUserIdOrderByTimestampDesc(userId);
    }
}
