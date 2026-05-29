package com.skillvibe.tutoring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCommentDTO {

    @NotBlank(message = "El comentario no puede estar vacío.")
    @Size(min = 1, max = 200, message = "El comentario debe tener entre 1 y 200 caracteres.")
    private String content;
}
