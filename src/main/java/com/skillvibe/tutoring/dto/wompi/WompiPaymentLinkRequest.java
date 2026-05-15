package com.skillvibe.tutoring.dto.wompi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * DTO de solicitud para crear un enlace de pago en Wompi.
 *
 * Referencia API: POST /v1/payment_links
 * Docs: https://docs.wompi.co/docs/colombia/canales-de-pago-link/
 */
@Getter
@Builder
public class WompiPaymentLinkRequest {

    /** Nombre visible del pago en el checkout de Wompi. */
    private String name;

    /** Descripción opcional del pago. */
    private String description;

    /**
     * Si es true, el enlace se puede usar una sola vez.
     * Recomendado para recargas individuales.
     */
    @JsonProperty("single_use")
    private boolean singleUse;

    /**
     * Moneda del pago.
     * Para Colombia SIEMPRE debe ser "COP".
     */
    private String currency;

    /**
     * Monto en centavos de COP.
     * Ejemplo: $50.000 COP = 5_000_000 centavos.
     */
    @JsonProperty("amount_in_cents")
    private Long amountInCents;

    /**
     * URL a la que Wompi redirige al usuario tras el pago exitoso.
     */
    @JsonProperty("redirect_url")
    private String redirectUrl;
}
