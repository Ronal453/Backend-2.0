package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CrearLoteRequest(
        @NotBlank(message = "La especie es obligatoria")
        String especie,

        @NotNull(message = "La cantidad inicial es obligatoria")
        @Min(value = 1, message = "La cantidad inicial debe ser mayor a 0")
        Integer cantidadInicial,

        @NotNull(message = "La fecha de siembra es obligatoria")
        LocalDate fechaSiembra,

        @NotNull(message = "El ID de la zona es obligatorio")
        Long idZona,

        String codigoLote,
        
        Long idProveedor,
        
        String estadoLote
) {}
