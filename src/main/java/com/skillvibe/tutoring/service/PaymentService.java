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
 * Servicio de pagos con Patrón Strategy.
 *
 * PaymentService NO conoce los detalles de ninguna pasarela de pago.
 * Delega toda la lógica de pago a la implementación activa de {@link PaymentProcessor},
 * actualmente {@code WompiPaymentProcessor} (Wompi Colombia - COP).
 *
 * Para cambiar de pasarela, solo se necesita:
 *   1. Crear otra implementación de PaymentProcessor.
 *   2. Marcarla con @Primary o usar @Qualifier en la inyección.
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
     * Crea un enlace de pago en Wompi delegando al PaymentProcessor activo.
     *
     * @param user   Usuario que realiza la recarga.
     * @param amount Monto en COP (pesos colombianos). Ej: 50000.0 = $50.000 COP
     * @return URL del checkout de Wompi.
     */
    public String createCheckoutSession(User user, Double amount) {
        log.info("Iniciando checkout para usuario: {} por ${} COP", user.getEmail(), amount);
        return paymentProcessor.createCheckoutSession(user, amount);
    }

    /**
     * Procesa un pago exitoso confirmado por el webhook de Wompi.
     * Acredita el saldo al usuario y registra la transacción en BD.
     *
     * La verificación de la firma SHA-256 se realiza en el PaymentController
     * ANTES de llamar a este método, garantizando que solo pagos legítimos
     * de Wompi lleguen aquí (no fraudes).
     *
     * @param userId         ID del usuario al que acreditar el saldo.
     * @param amount         Monto en COP a acreditar.
     * @param wompiPaymentId ID de la transacción en Wompi.
     */
    @SuppressWarnings("null")
    @Transactional
    public void processSuccessfulPayment(String userId, Double amount, String wompiPaymentId) {
        Long uId = Long.parseLong(userId);

        User user = userRepository.findById(uId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + uId));

        // Acreditar saldo en COP
        user.setBalance(user.getBalance() + amount);
        userRepository.save(user);

        // Registrar transacción
        Transaction transaction = Transaction.builder()
                .user(user)
                .amount(amount)
                .type(Transaction.TransactionType.LOAD)
                .description("Recarga de saldo via Wompi - $" + amount.longValue() + " COP")
                .externalPaymentId(wompiPaymentId)
                .build();

        transactionRepository.save(transaction);
        log.info("Pago Wompi procesado para {}: +${} COP (transaccion: {})",
                user.getFullName(), amount, wompiPaymentId);
    }

    /**
     * Devuelve el historial de transacciones de un usuario, ordenado del mas reciente al mas antiguo.
     */
    public List<Transaction> getHistory(Long userId) {
        return transactionRepository.findByUserIdOrderByTimestampDesc(userId);
    }
}
