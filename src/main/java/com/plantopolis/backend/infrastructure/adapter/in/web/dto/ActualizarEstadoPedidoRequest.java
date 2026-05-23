package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO de entrada para cambiar el estado de un pedido desde el panel admin.
 *
 * Se usa en: PATCH /api/admin/pedidos/{id}/estado
 *
 * El campo nuevoEstado debe ser exactamente uno de los valores definidos
 * en la tabla ESTADOPEDIDO de Oracle.
 *
 * */
@Schema(description = "Solicitud para cambiar el estado de un pedido")
public record ActualizarEstadoPedidoRequest(

        @Schema(
            description = "Nuevo estado del pedido. " +
                          "Valores permitidos: PREPARANDO, ENVIADO, ENTREGADO, CANCELADO. " +
                          "Solo las transiciones válidas son aceptadas.",
            example = "ENVIADO",
            allowableValues = {"PREPARANDO", "ENVIADO", "ENTREGADO", "CANCELADO"}
        )
        @NotBlank(message = "El nuevo estado es obligatorio")
        @Pattern(
            regexp = "PREPARANDO|ENVIADO|ENTREGADO|CANCELADO",
            message = "Estado inválido. Debe ser: PREPARANDO, ENVIADO, ENTREGADO o CANCELADO"
        )
        String nuevoEstado
) {}