package com.skillvibe.tutoring.controller;

import com.skillvibe.tutoring.dto.ApiResponse;
import com.skillvibe.tutoring.model.Transaccion;
import com.skillvibe.tutoring.model.User;
import com.skillvibe.tutoring.service.PagoService;
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
@RequestMapping("/api/pagos")
@Tag(name = "Pagos", description = "Endpoints para recarga de saldo e historial")
public class PagoController {

    private final PagoService pagoService;

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @PostMapping("/checkout")
    @Operation(summary = "Crear sesión de pago en Stripe")
    public ResponseEntity<ApiResponse<String>> createCheckout(@RequestBody Map<String, Double> payload, Authentication auth) throws Exception {
        User user = (User) auth.getPrincipal();
        Double amount = payload.get("amount");
        String url = pagoService.createCheckoutSession(user, amount);
        return ResponseEntity.ok(ApiResponse.success("URL de pago generada", url));
    }

    @GetMapping("/historial")
    @Operation(summary = "Obtener historial de transacciones")
    public ResponseEntity<ApiResponse<List<Transaccion>>> getHistorial(Authentication auth) {
        User user = (User) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Historial recuperado", pagoService.getHistory(user.getId())));
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
                    pagoService.processSuccessfulPayment(userId, amount, paymentId);
                }
            }
            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            log.error("Error en Webhook de Stripe: {}", e.getMessage());
            return ResponseEntity.status(400).body("Error");
        }
    }
}
