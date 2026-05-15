package com.skillvibe.tutoring.service.payment;

import com.skillvibe.tutoring.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Procesador de pagos SIMULADO para desarrollo y pruebas locales.
 *
 * Este procesador está activo cuando payment.provider=mock (valor por defecto).
 * NO llama a ninguna API externa — simplemente genera una URL local que,
 * al visitarse, acredita el saldo automáticamente simulando un pago exitoso.
 *
 * Para activar Wompi real en producción, cambiar en application.properties:
 *   payment.provider=wompi
 *
 * Flujo mock:
 *   1. createCheckoutSession() retorna GET /api/payments/mock-success?ref=SKILLVIBE-{userId}-{amount}
 *   2. El PaymentController tiene un endpoint GET /api/payments/mock-success
 *      que procesa el pago y redirige al frontend con ?payment=success.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "payment.provider", havingValue = "mock", matchIfMissing = true)
public class MockPaymentProcessor implements PaymentProcessor {

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public String createCheckoutSession(User user, Double amount) {
        String reference = WompiPaymentProcessor.buildReference(user.getId(), amount);

        // URL local que simula el pago: al visitarla acredita el saldo
        String mockUrl = "http://localhost:" + serverPort + "/api/payments/mock-success?ref=" + reference;

        log.warn("⚠️  MODO MOCK activo — pago NO real. Ref: {} | URL: {}", reference, mockUrl);
        log.warn("   Para activar Wompi real: payment.provider=wompi en application.properties");

        return mockUrl;
    }
}
