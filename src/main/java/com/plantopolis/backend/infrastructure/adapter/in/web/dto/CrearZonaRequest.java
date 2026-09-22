package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.plantopolis.backend.domain.model.Zona;

public record CrearZonaRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,
        @NotNull(message = "La capacidad máxima es obligatoria")
        @Min(value = 1, message = "La capacidad máxima debe ser al menos 1")
        Integer capacidadMaxima,
        @NotBlank(message = "El tipo de condición es obligatorio")
        String tipoCondicion,
        @NotBlank(message = "La exposición solar es obligatoria")
        String exposicionSolar
) {
    public Zona toDomain() {
        return Zona.builder()
                .nombre(this.nombre)
                .capacidadMaxima(this.capacidadMaxima)
                .tipoCondicion(this.tipoCondicion)
                .exposicionSolar(this.exposicionSolar)
                .build();
    }
}
