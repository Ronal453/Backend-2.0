package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para crear una cuenta de personal interno (Trabajador o Administrador)")
public record CrearTrabajadorRequest(

        @Schema(description = "Nombre completo del usuario", example = "Carlos Gómez")
        @NotBlank(message = "El nombre es obligatorio")
        String nombreCompleto,

        @Schema(description = "Correo único del usuario (será su usuario de login)",
                example = "carlos.gomez@plantopolis.com")
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Correo inválido")
        String correo,

        @Schema(description = "Contraseña inicial asignada por el admin. " +
                              "Se recomienda que el usuario la cambie en su primer ingreso.",
                example = "Temporal2024$")
        @NotBlank(message = "La contraseña inicial es obligatoria")
        @jakarta.validation.constraints.Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$", 
            message = "La contraseña debe tener mínimo 8 caracteres, al menos una mayúscula, un número y un carácter especial"
        )
        String passwordInicial
) {}