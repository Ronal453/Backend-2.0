package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Petición para solicitar el enlace de recuperación de contraseña (HU2).
 *
 * @param email Correo electrónico registrado del usuario
 */
public record RecuperarPasswordRequest(
        @Schema(description = "Correo electrónico registrado de la cuenta", example = "cliente@ejemplo.com")
        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El formato del correo electrónico no es válido")
        String email
) {
}
