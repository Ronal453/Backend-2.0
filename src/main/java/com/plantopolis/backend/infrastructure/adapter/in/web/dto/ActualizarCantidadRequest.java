package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ActualizarCantidadRequest(
        @NotNull @Min(value = 0, message = "La cantidad no puede ser negativa")
        Integer cantidad
) {}
