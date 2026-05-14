package com.skillvibe.tutoring.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class TutorRegistrationRequest {

    // --- Datos Básicos del Usuario ---
    @NotBlank(message = "El nombre completo es obligatorio")
    private String fullName;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato del correo es inválido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;

    // --- Datos del Perfil Profesional ---
    @NotBlank(message = "La biografía es obligatoria")
    @Size(min = 50, message = "La biografía debe tener al menos 50 caracteres para dar confianza")
    private String bio;

    @NotBlank(message = "La URL de la foto de perfil es obligatoria")
    private String profilePictureUrl;

    @NotBlank(message = "La URL de la cédula es obligatoria por seguridad")
    private String identityCardUrl;

    @NotBlank(message = "La URL del título o soporte académico es obligatoria")
    private String degreeUrl;

    @NotNull(message = "La tarifa por hora es obligatoria")
    @Positive(message = "La tarifa debe ser mayor a 0")
    private Double hourlyRate;

    @NotNull(message = "Los años de experiencia son obligatorios")
    @Min(value = 0, message = "Los años de experiencia no pueden ser negativos")
    private Integer yearsOfExperience;

    @NotEmpty(message = "Debes indicar al menos una materia que dominas")
    private List<String> subjects;
}
