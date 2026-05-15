package com.skillvibe.tutoring.service.payment;

import com.skillvibe.tutoring.dto.wompi.WompiPaymentLinkRequest;
import com.skillvibe.tutoring.dto.wompi.WompiPaymentLinkResponse;
import com.skillvibe.tutoring.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * Implementación del Patrón Strategy para Wompi (pasarela de pagos colombiana).
 *
 * Esta clase reemplaza a StripePaymentProcessor. Toda la lógica específica de
 * Wompi vive aquí, aislada del PaymentService y del PaymentController.
 *
 * Flujo de pago:
 *   1. {@link #createCheckoutSession} llama POST /v1/payment_links con el monto en COP.
 *   2. Wompi retorna un Payment Link con URL https://checkout.wompi.co/l/{id}.
 *   3. El frontend redirige al usuario a esa URL.
 *   4. Después del pago, Wompi envía un webhook a /api/payments/webhook.
 *   5. {@link #verifyWebhookSignature} verifica que el evento sea legítimo (SHA-256).
 *
 * API Reference: https://docs.wompi.co/docs/colombia/
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "payment.provider", havingValue = "wompi")
public class WompiPaymentProcessor implements PaymentProcessor {

    // ── Configuración inyectada desde application.properties ──────────────────
    @Value("${wompi.private.key}")
    private String wompiPrivateKey;

    @Value("${wompi.event.secret}")
    private String wompiEventSecret;

    @Value("${wompi.api.url}")
    private String wompiApiUrl;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    // ── RestClient (incluido en spring-boot-starter-web, no requiere dependencia extra) ──
    private final RestClient restClient;

    public WompiPaymentProcessor(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREAR PAYMENT LINK
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Crea un Payment Link de un solo uso en Wompi.
     *
     * @param user   El usuario que realiza la recarga.
     * @param amount El monto en COP (pesos colombianos, NO centavos).
     *               Ejemplo: 50000.0 → $50.000 COP
     * @return URL del checkout de Wompi (https://checkout.wompi.co/l/{id})
     */
    @SuppressWarnings("null")
    @Override
    public String createCheckoutSession(User user, Double amount) {
        log.info("Creando Payment Link Wompi para usuario {} por ${} COP", user.getEmail(), amount);

        // Wompi trabaja en centavos: $50.000 COP = 5.000.000 centavos
        long amountInCents = Math.round(amount * 100);

        // Referencia personalizada para identificar el pago en el webhook
        // Formato: SKILLVIBE-{userId}-{amountCOP}
        String reference = buildReference(user.getId(), amount);

        WompiPaymentLinkRequest request = WompiPaymentLinkRequest.builder()
                .name("Recarga SkillVibe - " + user.getFullName())
                .description("Recarga de crédito para tutorías en SkillVibe")
                .singleUse(true)
                .currency("COP")
                .amountInCents(amountInCents)
                .redirectUrl(frontendUrl + "/dashboard?payment=success&ref=" + reference)
                .build();

        WompiPaymentLinkResponse response = restClient.post()
                .uri(wompiApiUrl + "/payment_links")
                .header("Authorization", "Bearer " + wompiPrivateKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(WompiPaymentLinkResponse.class);

        if (response == null || response.getData() == null || response.getData().getUrl() == null) {
            log.error("Respuesta inválida de Wompi al crear Payment Link para usuario {}", user.getEmail());
            throw new RuntimeException("Error al generar el enlace de pago con Wompi");
        }

        String checkoutUrl = response.getData().getUrl();
        log.info("Payment Link creado exitosamente: {}", checkoutUrl);
        return checkoutUrl;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VERIFICACIÓN DE FIRMA SHA-256 (Seguridad de Webhooks)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verifica la firma SHA-256 del evento webhook de Wompi.
     *
     * Algoritmo según docs oficiales de Wompi:
     *   1. Extraer los valores de los campos en signature.properties (en orden).
     *   2. Concatenar: valores + timestamp + eventSecret.
     *   3. Calcular SHA-256 del string concatenado.
     *   4. Comparar con signature.checksum.
     *
     * @param propertyValues Valores de los campos de la transacción (en orden).
     * @param timestamp      Timestamp Unix del evento.
     * @param checksum       Hash recibido de Wompi (body o header X-Event-Checksum).
     * @return true si la firma es válida, false si el evento fue manipulado.
     */
    public boolean verifyWebhookSignature(List<String> propertyValues, Long timestamp, String checksum) {
        try {
            // Paso 1 y 2: Construir el string a hashear
            StringBuilder toHash = new StringBuilder();
            for (String value : propertyValues) {
                toHash.append(value);
            }
            toHash.append(timestamp);
            toHash.append(wompiEventSecret);

            // Paso 3: SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(toHash.toString().getBytes(StandardCharsets.UTF_8));
            String calculatedChecksum = HexFormat.of().formatHex(hashBytes);

            // Paso 4: Comparación segura (evita timing attacks)
            boolean valid = MessageDigest.isEqual(
                    calculatedChecksum.getBytes(StandardCharsets.UTF_8),
                    checksum.getBytes(StandardCharsets.UTF_8)
            );

            if (!valid) {
                log.warn("Firma de webhook Wompi inválida. Calculado: {}, Recibido: {}",
                        calculatedChecksum, checksum);
            }
            return valid;

        } catch (NoSuchAlgorithmException e) {
            // SHA-256 siempre está disponible en Java — esto nunca debería ocurrir
            throw new RuntimeException("SHA-256 no disponible en el entorno Java", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILIDADES
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Construye la referencia personalizada del pago.
     * Formato: SKILLVIBE-{userId}-{amountCOP}
     * Ejemplo: SKILLVIBE-42-50000.0
     */
    public static String buildReference(Long userId, Double amountCOP) {
        return "SKILLVIBE-" + userId + "-" + amountCOP;
    }

    /**
     * Extrae el userId de una referencia Wompi.
     * Ejemplo: "SKILLVIBE-42-50000.0" → 42L
     */
    public static Long extractUserId(String reference) {
        String[] parts = reference.split("-");
        return Long.parseLong(parts[1]);
    }

    /**
     * Extrae el monto COP de una referencia Wompi.
     * Ejemplo: "SKILLVIBE-42-50000.0" → 50000.0
     */
    public static Double extractAmount(String reference) {
        String[] parts = reference.split("-");
        return Double.parseDouble(parts[2]);
    }
}
