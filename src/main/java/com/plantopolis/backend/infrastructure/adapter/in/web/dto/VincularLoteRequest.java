package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;

public record VincularLoteRequest(
        @NotNull(message = "El ID del producto es obligatorio")
        Long idProducto
) {}
