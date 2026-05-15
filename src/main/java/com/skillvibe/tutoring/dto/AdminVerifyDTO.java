package com.skillvibe.tutoring.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminVerifyDTO {

    @NotNull(message = "El estado de verificación es obligatorio")
    private Boolean verified;

    /**
     * Motivo opcional, especialmente útil si verified=false (rechazado).
     */
    private String reason;
}
