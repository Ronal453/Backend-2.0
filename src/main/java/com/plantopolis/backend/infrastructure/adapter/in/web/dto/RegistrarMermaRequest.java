package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record RegistrarMermaRequest(
        @NotNull(message = "El ID del lote es obligatorio")
        Long idLote,

        @NotNull(message = "La causa de la merma es obligatoria")
        Long idCausa,

        @NotNull(message = "La cantidad perdida es obligatoria")
        @Min(value = 1, message = "La cantidad perdida debe ser al menos 1")
        Integer cantidadPerdida,

        LocalDate fechaMerma,

        String observaciones
) {}
