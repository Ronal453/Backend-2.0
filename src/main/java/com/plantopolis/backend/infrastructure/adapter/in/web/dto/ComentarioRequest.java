package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ComentarioRequest(
        @NotBlank(message = "El comentario no puede estar vacío")
        String comentario
) {}
