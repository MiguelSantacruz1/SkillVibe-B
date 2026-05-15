package com.skillvibe.tutoring.dto.wompi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta de la API de Wompi al crear un Payment Link.
 *
 * La API retorna un objeto "data" con los campos del enlace generado.
 * Usamos @JsonIgnoreProperties para ignorar campos no relevantes.
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WompiPaymentLinkResponse {

    /** Envuelve los datos reales del payment link. */
    private WompiPaymentLinkData data;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WompiPaymentLinkData {

        /** ID único del payment link en Wompi. */
        private String id;

        /** Nombre del payment link. */
        private String name;

        /** Estado del link ("ACTIVE", "INACTIVE"). */
        private String status;

        /** URL del checkout: https://checkout.wompi.co/l/<id> */
        @JsonProperty("url")
        private String url;
    }
}
