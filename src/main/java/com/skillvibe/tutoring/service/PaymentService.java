package com.skillvibe.tutoring.service;

import com.skillvibe.tutoring.model.Transaction;
import com.skillvibe.tutoring.model.User;
import com.skillvibe.tutoring.repository.TransactionRepository;
import com.skillvibe.tutoring.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Servicio de pagos.
 * Ahora implementa una simulación directa en Pesos Colombianos (COP) 
 * sin depender de Wompi ni pasarelas externas.
 */
@Slf4j
@Service
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public PaymentService(TransactionRepository transactionRepository,
                       UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    /**
     * Simula un pago exitoso.
     * Acredita el saldo al usuario y registra la transacción en BD.
     *
     * @param userId         ID del usuario al que acreditar el saldo.
     * @param amount         Monto en COP a acreditar.
     */
    @Transactional
    public void simulatePayment(Long userId, Double amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + userId));

        // Acreditar saldo en COP
        user.setBalance(user.getBalance() + amount);
        userRepository.save(user);

        // Generar un ID de transacción ficticio
        String mockPaymentId = "SIMULATION-" + UUID.randomUUID().toString().substring(0, 8);

        // Registrar transacción
        Transaction transaction = Transaction.builder()
                .user(user)
                .amount(amount)
                .type(Transaction.TransactionType.LOAD)
                .description("Recarga de saldo (Simulación) - $" + amount.longValue() + " COP")
                .externalPaymentId(mockPaymentId)
                .build();

        transactionRepository.save(transaction);
        log.info("Pago simulado procesado para {}: +${} COP (transaccion: {})",
                user.getFullName(), amount, mockPaymentId);
    }

    /**
     * Devuelve el historial de transacciones de un usuario, ordenado del mas reciente al mas antiguo.
     */
    public List<Transaction> getHistory(Long userId) {
        return transactionRepository.findByUserIdOrderByTimestampDesc(userId);
    }
}
