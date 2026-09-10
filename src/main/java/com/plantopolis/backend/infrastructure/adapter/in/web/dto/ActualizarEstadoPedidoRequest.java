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
                        "Valores permitidos: EN_PREPARACION, ENVIADO, ENTREGADO, CANCELADO. " +
                        "Solo las transiciones válidas son aceptadas.",
            example = "ENVIADO",
            allowableValues = {"EN_PREPARACION", "ENVIADO", "ENTREGADO", "CANCELADO"}
        )
        @NotBlank(message = "El nuevo estado es obligatorio")
        @Pattern(
            regexp = "EN_PREPARACION|ENVIADO|ENTREGADO|CANCELADO",
            message = "Estado inválido. Debe ser: EN_PREPARACION, ENVIADO, ENTREGADO o CANCELADO"
        )
        String nuevoEstado
) {}