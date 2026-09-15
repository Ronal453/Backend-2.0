package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para crear una cuenta de trabajador (solo ADMINISTRADOR)")
public record CrearTrabajadorRequest(

        @Schema(description = "Nombre completo del trabajador", example = "Carlos Gómez")
        @NotBlank(message = "El nombre es obligatorio")
        String nombreCompleto,

        @Schema(description = "Correo único del trabajador (será su usuario de login)",
                example = "carlos.gomez@plantopolis.com")
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Correo inválido")
        String correo,

        @Schema(description = "Contraseña inicial asignada por el admin. " +
                              "Se recomienda que el trabajador la cambie en su primer ingreso.",
                example = "Temporal2024")
        @NotBlank(message = "La contraseña inicial es obligatoria")
        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
        String passwordInicial
) {}