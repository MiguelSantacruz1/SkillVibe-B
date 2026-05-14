package com.skillvibe.tutoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequestDTO {
    private Long tutorId;
    private String materia;
    private String descripcion;
    private LocalDateTime fechaHora;
}
