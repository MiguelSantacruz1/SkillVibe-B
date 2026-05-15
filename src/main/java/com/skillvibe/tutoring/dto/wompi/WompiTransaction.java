package com.skillvibe.tutoring.dto.wompi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de la transacción contenida en el evento webhook de Wompi.
 *
 * Campos clave:
 * - id: ID único de la transacción en Wompi.
 * - reference: Referencia personalizada que enviamos al crear el payment link.
 *   Usamos el formato "SKILLVIBE-{userId}-{amountCOP}" para identificar el pago.
 * - status: "APPROVED" | "DECLINED" | "VOIDED" | "ERROR" | "PENDING"
 * - amountInCents: Monto total en centavos de COP.
 * - currency: Siempre "COP".
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WompiTransaction {

    /** ID de la transacción en Wompi (guardado como externalPaymentId). */
    private String id;

    /**
     * Referencia personalizada enviada al crear el payment link.
     * Formato: "SKILLVIBE-{userId}-{amountCOP}"
     */
    private String reference;

    /**
     * Estado de la transacción:
     * APPROVED  → Pago exitoso, acreditar saldo.
     * DECLINED  → Pago rechazado.
     * VOIDED    → Transacción anulada.
     * ERROR     → Error del sistema.
     * PENDING   → En proceso.
     */
    private String status;

    /** Monto pagado en centavos de COP. */
    @JsonProperty("amount_in_cents")
    private Long amountInCents;

    /** Moneda de la transacción. Siempre "COP". */
    private String currency;

    /** Email del pagador (si lo proporcionó). */
    @JsonProperty("customer_email")
    private String customerEmail;
}
