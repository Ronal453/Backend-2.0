package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Petición para restablecer la contraseña a partir de un token temporal válido (HU2).
 * Aplica estrictamente la política corporativa de contraseñas de Plantopolis:
 * mínimo 8 caracteres, al menos una mayúscula, un número y un carácter especial (@$!%*?&).
 *
 * @param token Token JWT temporal firmado recibido por correo
 * @param nuevaPassword Nueva contraseña seleccionada por el usuario
 */
public record ResetearPasswordRequest(
        @Schema(description = "Token temporal de recuperación recibido por enlace de correo")
        @NotBlank(message = "El token de recuperación es obligatorio")
        String token,

        @Schema(description = "Nueva contraseña para la cuenta (mínimo 8 caracteres, 1 mayúscula, 1 número y 1 carácter especial)", example = "PlantaSegura2026*")
        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
                message = "La contraseña debe tener mínimo 8 caracteres, al menos una mayúscula, un número y un carácter especial (@$!%*?&)"
        )
        String nuevaPassword
) {
}
