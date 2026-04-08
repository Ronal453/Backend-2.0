package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AgregarItemRequest(
        @NotNull(message = "El producto es obligatorio")
        Long idProducto,

        @NotNull @Min(value = 1, message = "La cantidad mínima es 1")
        Integer cantidad
) {}