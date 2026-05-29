package com.skillvibe.tutoring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreatePostDTO {

    @NotBlank(message = "El contenido no puede estar vacío.")
    @Size(min = 1, max = 500, message = "El contenido debe tener entre 1 y 500 caracteres.")
    private String content;

    private String imageUrl;
}
