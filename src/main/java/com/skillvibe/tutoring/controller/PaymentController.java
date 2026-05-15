package com.skillvibe.tutoring.controller;

import com.skillvibe.tutoring.dto.ApiResponse;
import com.skillvibe.tutoring.model.Transaction;
import com.skillvibe.tutoring.model.User;
import com.skillvibe.tutoring.security.UserPrincipal;
import com.skillvibe.tutoring.service.PaymentService;
import com.skillvibe.tutoring.service.UserService;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@Tag(name = "Pagos", description = "Endpoints para recarga de saldo e historial")
public class PaymentController {

    private final PaymentService PaymentService;
    private final UserService userService;

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    public PaymentController(PaymentService PaymentService, UserService userService) {
        this.PaymentService = PaymentService;
        this.userService = userService;
    }

    @PostMapping("/checkout")
    @Operation(summary = "Crear sesión de pago en Stripe")
    public ResponseEntity<ApiResponse<String>> createCheckout(@RequestBody Map<String, Double> payload, Authentication auth) throws Exception {
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        // Cargamos el objeto User real solo porque PaymentService lo necesita para Stripe
        User user = userService.findById(principal.getId());
        
        Double amount = payload.get("amount");
        String url = PaymentService.createCheckoutSession(user, amount);
        return ResponseEntity.ok(ApiResponse.success("URL de pago generada", url));
    }

    @GetMapping("/historial")
    @Operation(summary = "Obtener historial de Transactiones")
    public ResponseEntity<ApiResponse<List<Transaction>>> getHistorial(Authentication auth) {
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Historial recuperado", PaymentService.getHistory(principal.getId())));
    }

    @PostMapping("/webhook")
    @Operation(summary = "Webhook para procesar pagos de Stripe (Uso interno)")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, endpointSecret);

            if ("checkout.session.completed".equals(event.getType())) {
                Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
                if (session != null) {
                    String userId = session.getMetadata().get("userId");
                    String amount = session.getMetadata().get("amount");
                    String paymentId = session.getPaymentIntent();
                    PaymentService.processSuccessfulPayment(userId, amount, paymentId);
                }
            }
            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            log.error("Error en Webhook de Stripe: {}", e.getMessage());
            return ResponseEntity.status(400).body("Error");
        }
    }
}
