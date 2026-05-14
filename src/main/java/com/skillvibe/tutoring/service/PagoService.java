package com.skillvibe.tutoring.service;

import com.skillvibe.tutoring.model.Transaccion;
import com.skillvibe.tutoring.model.User;
import com.skillvibe.tutoring.repository.TransaccionRepository;
import com.skillvibe.tutoring.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Service
public class PagoService {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private final TransaccionRepository transaccionRepository;
    private final UserRepository userRepository;

    public PagoService(TransaccionRepository transaccionRepository, UserRepository userRepository) {
        this.transaccionRepository = transaccionRepository;
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    public String createCheckoutSession(User user, Double amount) throws Exception {
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

        // Registrar transacción
        Transaccion transaccion = Transaccion.builder()
                .user(user)
                .amount(amt)
                .type(Transaccion.TransactionType.LOAD)
                .description("Recarga de saldo vía Stripe")
                .stripePaymentId(paymentId)
                .build();
        
        transaccionRepository.save(transaccion);
        log.info("Pago procesado con éxito para el usuario {}: +${}", user.getFullName(), amt);
    }

    public List<Transaccion> getHistory(Long userId) {
        return transaccionRepository.findByUserIdOrderByTimestampDesc(userId);
    }
}
