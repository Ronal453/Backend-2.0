package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Datos necesarios para procesar el checkout del carrito")
public record CheckoutRequest(

        @Schema(
            description = "ID del método de pago seleccionado. " +
                          "1=TARJETA_CREDITO, 2=TARJETA_DEBITO, 3=TRANSFERENCIA, 4=EFECTIVO",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "El método de pago es obligatorio")
        Long idMetodoPago,

        @Schema(
            description = "Dirección completa de envío del pedido",
            example = "Calle 123 #45-67, Bogotá, Cundinamarca",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "La dirección de envío es obligatoria")
        String direccionEnvio
) {}
