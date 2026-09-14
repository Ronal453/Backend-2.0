package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CambiarEstadoLoteRequest(
        @NotBlank(message = "El nuevo estado del lote es obligatorio")
        String nuevoEstado,
        String observaciones
) {}
