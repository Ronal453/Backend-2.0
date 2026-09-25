package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(

    @NotBlank(message = "El nombre es obligatorio")
    String nombre,

    @NotBlank @Email(message = "Email inválido")
    String email,

    @NotBlank 
    @jakarta.validation.constraints.Pattern(
        regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$", 
        message = "La contraseña debe tener mínimo 8 caracteres, al menos una mayúscula, un número y un carácter especial"
    )
    String password,

    // Opcionales — no llevan @NotBlank
    String telefono,

    String direccion
) {}