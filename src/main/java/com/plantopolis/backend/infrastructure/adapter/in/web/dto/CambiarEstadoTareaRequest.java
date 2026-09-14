package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CambiarEstadoTareaRequest(
        @NotBlank(message = "El nuevo estado es obligatorio")
        String nuevoEstado,
        String comentario
) {}
