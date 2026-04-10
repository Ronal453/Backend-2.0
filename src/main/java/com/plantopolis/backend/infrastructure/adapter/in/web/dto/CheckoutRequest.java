package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
    @NotNull(message = "El método de pago es obligatorio")
    Long idMetodoPago,

    @NotBlank(message = "La dirección de envío es obligatoria")
    String direccionEnvio
) {}
