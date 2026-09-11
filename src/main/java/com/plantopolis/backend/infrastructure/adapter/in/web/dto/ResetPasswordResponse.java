package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado de resetear la contraseña de un usuario. " +
                      "La contraseña temporal solo se devuelve esta vez; no se vuelve a mostrar.")
public record ResetPasswordResponse(
        String mensaje,
        String passwordTemporal
) {}