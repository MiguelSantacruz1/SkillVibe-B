package com.skillvibe.tutoring.dto.wompi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de la firma de seguridad incluida en cada evento webhook de Wompi.
 *
 * Wompi usa SHA-256 para firmar los eventos. La verificación es:
 * 1. Concatenar los valores de los campos en signature.properties (en ese orden).
 * 2. Concatenar el timestamp del evento.
 * 3. Concatenar el Event Secret de tu dashboard.
 * 4. Calcular SHA-256 del string resultante.
 * 5. Comparar con signature.checksum (o el header X-Event-Checksum).
 *
 * Docs: https://docs.wompi.co/docs/colombia/eventos/#verificar-la-firma
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WompiSignature {

    /**
     * Hash SHA-256 calculado por Wompi.
     * Equivale al header HTTP "X-Event-Checksum".
     */
    private String checksum;

    /**
     * Lista de propiedades de la transacción que se incluyeron en el hash.
     * El orden importa. Ejemplo: ["transaction.id", "transaction.status",
     * "transaction.amount_in_cents", "transaction.currency"]
     *
     * IMPORTANTE: Usar siempre esta lista del evento (no hardcodearla).
     */
    private List<String> properties;
}
