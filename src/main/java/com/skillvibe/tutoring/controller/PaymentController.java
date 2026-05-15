package com.skillvibe.tutoring.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillvibe.tutoring.dto.ApiResponse;
import com.skillvibe.tutoring.dto.wompi.WompiTransaction;
import com.skillvibe.tutoring.dto.wompi.WompiWebhookEvent;
import com.skillvibe.tutoring.model.Transaction;
import com.skillvibe.tutoring.model.User;
import com.skillvibe.tutoring.security.UserPrincipal;
import com.skillvibe.tutoring.service.PaymentService;
import com.skillvibe.tutoring.service.UserService;
import com.skillvibe.tutoring.service.payment.WompiPaymentProcessor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controlador de pagos.
 *
 * Endpoints:
 *   POST /api/payments/checkout       → Crea un link de pago (Wompi o Mock).
 *   GET  /api/payments/historial      → Historial de transacciones del usuario.
 *   POST /api/payments/webhook        → Webhook de Wompi (verif. SHA-256).
 *   GET  /api/payments/mock-success   → Simula un pago exitoso (solo modo mock/dev).
 *
 * Modo activo controlado por: payment.provider=mock|wompi en application.properties.
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
@Tag(name = "Pagos", description = "Endpoints para recarga de saldo e historial (COP)")
public class PaymentController {

    private final PaymentService paymentService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    // Opcional: solo existe cuando payment.provider=wompi
    private final Optional<WompiPaymentProcessor> wompiProcessor;

    @Value("${payment.provider:mock}")
    private String paymentProvider;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    public PaymentController(PaymentService paymentService,
                             UserService userService,
                             ObjectMapper objectMapper,
                             @Autowired(required = false) WompiPaymentProcessor wompiProcessor) {
        this.paymentService = paymentService;
        this.userService = userService;
        this.objectMapper = objectMapper;
        this.wompiProcessor = Optional.ofNullable(wompiProcessor);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/payments/checkout
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Crea un enlace de pago y retorna la URL del checkout.
     * En modo mock: URL local que simula el pago.
     * En modo wompi: URL del checkout real de Wompi.
     *
     * @param payload JSON con campo "amount" en COP. Ej: { "amount": 50000.0 }
     */
    @PostMapping("/checkout")
    @Operation(summary = "Crear enlace de pago (COP)",
               description = "Genera un link de pago. Usa Wompi en producción o Mock en desarrollo.")
    public ResponseEntity<ApiResponse<String>> createCheckout(
            @RequestBody Map<String, Double> payload,
            Authentication auth) {

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        User user = userService.findById(principal.getId());

        Double amount = payload.get("amount");
        if (amount == null || amount <= 0) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("El monto debe ser mayor a 0 COP"));
        }

        String url = paymentService.createCheckoutSession(user, amount);
        return ResponseEntity.ok(ApiResponse.success("Enlace de pago generado", url));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/payments/historial
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/historial")
    @Operation(summary = "Obtener historial de transacciones del usuario autenticado")
    public ResponseEntity<ApiResponse<List<Transaction>>> getHistorial(Authentication auth) {
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        return ResponseEntity.ok(
                ApiResponse.success("Historial recuperado",
                        paymentService.getHistory(principal.getId()))
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/payments/mock-success  (Solo modo desarrollo)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Simula un pago exitoso para pruebas locales.
     *
     * Flujo:
     *   1. El MockPaymentProcessor genera una URL con ?ref=SKILLVIBE-{userId}-{amount}.
     *   2. El frontend redirige al usuario a esta URL (como si fuera Wompi).
     *   3. Este endpoint extrae userId y amount, acredita el saldo y redirige al dashboard.
     *
     * ⚠️  SOLO disponible cuando payment.provider=mock.
     *     En producción (payment.provider=wompi) retorna 403 Forbidden.
     *
     * @param ref      Referencia del pago. Formato: SKILLVIBE-{userId}-{amountCOP}
     * @param response HttpServletResponse para hacer el redirect al frontend.
     */
    @GetMapping("/mock-success")
    @Operation(summary = "[SOLO DEV] Simular pago exitoso",
               description = "Acredita el saldo automáticamente para pruebas locales. Deshabilitado en producción.")
    public void mockPaymentSuccess(@RequestParam String ref,
                                   HttpServletResponse response) throws IOException {

        if (!"mock".equals(paymentProvider)) {
            log.warn("Intento de acceso a /mock-success en modo produccion (provider={}). Rechazado.", paymentProvider);
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Este endpoint solo está disponible en modo de desarrollo (payment.provider=mock)");
            return;
        }

        try {
            if (!ref.startsWith("SKILLVIBE-")) {
                log.warn("Referencia mock inválida: {}", ref);
                response.sendRedirect(frontendUrl + "/dashboard?payment=error");
                return;
            }

            Long userId = WompiPaymentProcessor.extractUserId(ref);
            Double amountCOP = WompiPaymentProcessor.extractAmount(ref);

            log.info("🧪 [MOCK] Simulando pago exitoso — usuario: {}, monto: ${} COP", userId, amountCOP);

            paymentService.processSuccessfulPayment(
                    userId.toString(),
                    amountCOP,
                    "mock-" + System.currentTimeMillis()
            );

            log.info("🧪 [MOCK] Saldo acreditado OK — redirigiendo al dashboard");
            response.sendRedirect(frontendUrl + "/dashboard?payment=success");

        } catch (Exception e) {
            log.error("Error en mock-success: {}", e.getMessage(), e);
            response.sendRedirect(frontendUrl + "/dashboard?payment=error");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/payments/webhook  (Wompi — verificado con SHA-256)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Recibe y procesa eventos webhook enviados por Wompi.
     *
     * Flujo de seguridad:
     *   1. Parsear el JSON del evento.
     *   2. Extraer signature.properties y timestamp.
     *   3. Recuperar los valores de los campos en ese orden desde la transacción.
     *   4. Calcular SHA-256(valores + timestamp + eventSecret).
     *   5. Comparar con X-Event-Checksum del header.
     *   6. Solo si la firma es válida → procesar el evento.
     *
     * Solo procesamos eventos de tipo "transaction.updated" con status "APPROVED".
     *
     * @param payload  Cuerpo raw del evento (JSON string).
     * @param checksum Header X-Event-Checksum enviado por Wompi.
     */
    @PostMapping("/webhook")
    @Operation(summary = "Webhook de Wompi (uso interno — no llamar directamente)",
               description = "Recibe notificaciones de pago de Wompi con verificación SHA-256.")
    public ResponseEntity<String> handleWompiWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Event-Checksum", required = false) String checksum) {

        log.info("Webhook Wompi recibido. Checksum: {}", checksum);

        // Si estamos en modo mock, no hay webhooks reales que procesar
        if ("mock".equals(paymentProvider)) {
            log.info("Modo mock activo — webhook ignorado");
            return ResponseEntity.ok("Mock mode — webhook ignored");
        }

        if (!wompiProcessor.isPresent()) {
            log.error("Webhook Wompi recibido pero WompiPaymentProcessor no está activo");
            return ResponseEntity.status(503).body("Wompi processor no disponible");
        }

        try {
            // ── 1. Parsear el evento ──
            WompiWebhookEvent event = objectMapper.readValue(payload, WompiWebhookEvent.class);

            if (event.getSignature() == null || event.getTimestamp() == null) {
                log.warn("Webhook Wompi rechazado: falta firma o timestamp");
                return ResponseEntity.badRequest().body("Evento malformado: falta firma o timestamp");
            }

            WompiTransaction wompiTx = event.getData() != null ? event.getData().getTransaction() : null;
            if (wompiTx == null) {
                log.warn("Webhook Wompi sin datos de transacción");
                return ResponseEntity.ok("Evento sin transacción — ignorado");
            }

            // ── 2. Construir los valores de propiedades en el orden de signature.properties ──
            List<String> propertyValues = extractPropertyValues(wompiTx, event.getSignature().getProperties());

            // ── 3. Verificar firma SHA-256 ──
            String checksumToVerify = (checksum != null) ? checksum : event.getSignature().getChecksum();
            if (checksumToVerify == null || !wompiProcessor.get().verifyWebhookSignature(
                    propertyValues, event.getTimestamp(), checksumToVerify)) {
                log.error("Firma de webhook Wompi INVÁLIDA — posible fraude. Evento rechazado.");
                return ResponseEntity.badRequest().body("Firma inválida");
            }

            // ── 4. Procesar solo eventos de pago aprobado ──
            if (!"transaction.updated".equals(event.getEvent())) {
                log.info("Evento Wompi '{}' ignorado", event.getEvent());
                return ResponseEntity.ok("Evento ignorado");
            }

            if (!"APPROVED".equals(wompiTx.getStatus())) {
                log.info("Transacción Wompi {} con estado '{}' — sin acreditar", wompiTx.getId(), wompiTx.getStatus());
                return ResponseEntity.ok("Transacción no aprobada — ignorada");
            }

            // ── 5. Extraer userId y monto desde la referencia ──
            String reference = wompiTx.getReference();
            if (reference == null || !reference.startsWith("SKILLVIBE-")) {
                log.warn("Referencia Wompi no reconocida: {}", reference);
                return ResponseEntity.ok("Referencia no reconocida — ignorada");
            }

            Long userId = WompiPaymentProcessor.extractUserId(reference);
            Double amountCOP = WompiPaymentProcessor.extractAmount(reference);

            // ── 6. Acreditar saldo ──
            paymentService.processSuccessfulPayment(userId.toString(), amountCOP, wompiTx.getId());

            log.info("Webhook Wompi procesado OK — usuario: {}, monto: ${} COP", userId, amountCOP);
            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("Error procesando webhook Wompi: {}", e.getMessage(), e);
            return ResponseEntity.ok("Error interno procesado");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILIDAD PRIVADA
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Extrae los valores de la transacción en el orden indicado por signature.properties.
     * Wompi indica dinámicamente qué campos usar para el hash — no hardcodear el orden.
     */
    private List<String> extractPropertyValues(WompiTransaction tx, List<String> properties) {
        List<String> values = new ArrayList<>();
        if (properties == null) return values;

        for (String prop : properties) {
            String value = switch (prop) {
                case "transaction.id"              -> tx.getId();
                case "transaction.status"          -> tx.getStatus();
                case "transaction.amount_in_cents" -> tx.getAmountInCents() != null
                                                        ? tx.getAmountInCents().toString() : "";
                case "transaction.currency"        -> tx.getCurrency();
                case "transaction.reference"       -> tx.getReference();
                default -> {
                    log.warn("Propiedad de firma Wompi desconocida: {}", prop);
                    yield "";
                }
            };
            values.add(value != null ? value : "");
        }
        return values;
    }
}
