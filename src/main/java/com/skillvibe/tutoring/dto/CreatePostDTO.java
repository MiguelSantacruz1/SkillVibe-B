package com.skillvibe.tutoring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreatePostDTO {

    @NotBlank(message = "El contenido no puede estar vacío.")
    @Size(min = 1, max = 2000, message = "El contenido debe tener entre 1 y 2000 caracteres.")
    private String content;

    @Size(max = 512)
    private String imageUrl;
}
