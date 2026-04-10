package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import com.plantopolis.backend.domain.model.Pedido;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Respuesta con la información completa de un pedido")
public record PedidoResponse(

        @Schema(description = "ID único del pedido", example = "1")
        Long idPedido,

        @Schema(description = "Número legible del pedido", example = "PL-20241215-4823")
        String numeroPedido,

        @Schema(description = "Estado actual: PENDIENTE, PREPARANDO, ENVIADO, ENTREGADO, CANCELADO",
                example = "PENDIENTE")
        String estado,

        @Schema(description = "Fecha y hora en que se realizó el pedido")
        LocalDateTime fechaPedido,

        @Schema(description = "Dirección de envío", example = "Calle 123, Bogotá")
        String direccionEnvio,

        @Schema(description = "Total del pedido en pesos colombianos", example = "85000.00")
        BigDecimal total,

        @Schema(description = "Lista de productos incluidos en el pedido")
        List<DetalleResponse> detalles,

        @Schema(description = "Información del pago realizado")
        PagoInfo pago
) {

    // ── Detalle de cada producto ──────────────────────────────
    @Schema(description = "Detalle de un producto dentro del pedido")
    public record DetalleResponse(
            @Schema(description = "ID del producto", example = "5")
            Long idProducto,

            @Schema(description = "Nombre del producto", example = "Pothos Dorado")
            String nombreProducto,

            @Schema(description = "URL de la imagen del producto")
            String imagenUrl,

            @Schema(description = "Cantidad comprada", example = "2")
            Integer cantidad,

            @Schema(description = "Precio por unidad al momento de la compra", example = "25000.00")
            BigDecimal precioUnitario,

            @Schema(description = "Subtotal (cantidad × precio)", example = "50000.00")
            BigDecimal subtotal
    ) {}

    // ── Info del pago ─────────────────────────────────────────
    @Schema(description = "Información del pago asociado al pedido")
    public record PagoInfo(
            @Schema(description = "Método de pago usado", example = "TARJETA_CREDITO")
            String metodoPago,

            @Schema(description = "Estado del pago", example = "APROBADO")
            String estadoPago,

            @Schema(description = "Monto total pagado", example = "85000.00")
            BigDecimal monto,

            @Schema(description = "Fecha y hora del pago")
            LocalDateTime fechaPago
    ) {}

    // ── Builder estático desde el dominio ─────────────────────
    public static PedidoResponse from(Pedido p) {
        List<DetalleResponse> detalles = p.getDetalles() != null
                ? p.getDetalles().stream().map(d -> new DetalleResponse(
                        d.getIdProducto(),
                        d.getNombreProducto(),
                        d.getImagenUrl(),
                        d.getCantidad(),
                        d.getPrecioUnitario(),
                        d.getSubtotal()
                  )).toList()
                : List.of();

        PagoInfo pago = p.getPago() != null
                ? new PagoInfo(
                        p.getPago().getNombreMetodo(),
                        p.getPago().getEstadoPagoDescripcion(),
                        p.getPago().getMonto(),
                        p.getPago().getFechaPago())
                : null;

        return new PedidoResponse(
                p.getIdPedido(),
                p.getNumeroPedido(),
                p.getEstadoDescripcion(),
                p.getFechaPedido(),
                p.getDireccionEnvio(),
                p.getTotal(),
                detalles,
                pago);
    }
}