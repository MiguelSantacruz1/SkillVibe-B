package com.skillvibe.tutoring.service.payment;

import com.skillvibe.tutoring.model.User;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Implementación concreta del Patrón Strategy para Stripe.
 *
 * Toda la lógica específica de la SDK de Stripe vive aquí, aislada
 * de PaymentService. Si se agrega un PayPalPaymentProcessor en el futuro,
 * PaymentService no requiere ningún cambio.
 */
@Slf4j
@Component
public class StripePaymentProcessor implements PaymentProcessor {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
        log.info("StripePaymentProcessor inicializado correctamente.");
    }

    @Override
    public String createCheckoutSession(User user, Double amount) throws Exception {
        log.info("Creando sesión de Stripe para usuario: {} por ${}", user.getEmail(), amount);

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(frontendUrl + "/dashboard?payment=success")
                .setCancelUrl(frontendUrl + "/dashboard?payment=cancel")
                .setCustomerEmail(user.getEmail())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("usd")
                                .setUnitAmount((long) (amount * 100)) // Stripe usa centavos
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Carga de Saldo - SkillVibe")
                                        .setDescription("Recarga de crédito para tutorías")
                                        .build())
                                .build())
                        .build())
                .putMetadata("userId", user.getId().toString())
                .putMetadata("amount", amount.toString())
                .build();

        Session session = Session.create(params);
        return session.getUrl();
    }
}
