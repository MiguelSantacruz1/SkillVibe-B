package com.skillvibe.tutoring.dto.wompi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO raíz del evento webhook que Wompi envía a nuestro endpoint.
 *
 * Estructura del evento:
 * {
 *   "event": "transaction.updated",
 *   "data": { "transaction": { ... } },
 *   "sent_at": "2024-01-01T00:00:00Z",
 *   "timestamp": 1704067200,
 *   "signature": { "checksum": "abc...", "properties": ["transaction.id", ...] }
 * }
 *
 * Docs: https://docs.wompi.co/docs/colombia/eventos/
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WompiWebhookEvent {

    /** Tipo de evento, ej: "transaction.updated". */
    private String event;

    /** Datos del evento (contiene la transacción). */
    private WompiEventData data;

    /**
     * Timestamp Unix del evento (usado en la verificación de firma).
     * Es un entero, no un string.
     */
    private Long timestamp;

    /** Firma para verificar autenticidad del evento. */
    private WompiSignature signature;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WompiEventData {
        /** La transacción asociada al evento. */
        private WompiTransaction transaction;
    }
}
