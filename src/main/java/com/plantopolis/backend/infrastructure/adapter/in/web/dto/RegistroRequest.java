package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(

    @NotBlank(message = "El nombre es obligatorio")
    String nombre,

    @NotBlank @Email(message = "Email inválido")
    String email,

    @NotBlank @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    String password,

    // Opcionales — no llevan @NotBlank
    String telefono,

    String direccion
) {}