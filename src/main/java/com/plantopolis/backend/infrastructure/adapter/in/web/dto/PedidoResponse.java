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

        @Schema(description = "Estado actual del pedido", example = "PENDIENTE")
        String estado,

        @Schema(description = "Fecha y hora en que se realizó el pedido")
        LocalDateTime fechaPedido,

        @Schema(description = "Dirección de envío", example = "Calle 123, Bogotá")
        String direccionEnvio,

        @Schema(description = "Total del pedido", example = "85000.00")
        BigDecimal total,

        @Schema(description = "Lista de productos del pedido")
        List<DetalleResponse> detalles,

        @Schema(description = "Información del pago")
        PagoInfo pago,

        // ── [NUEVO] Datos del cliente para el panel admin ──────────────────
        @Schema(description = "Nombre completo del cliente",
                example = "Juan Pérez")
        String nombreCliente,

        @Schema(description = "Correo electrónico del cliente",
                example = "juan@email.com")
        String emailCliente

) {

    // ── Detalle de cada producto ──────────────────────────────────────────
    @Schema(description = "Detalle de un producto dentro del pedido")
    public record DetalleResponse(
            Long       idProducto,
            String     nombreProducto,
            String     imagenUrl,
            Integer    cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal
    ) {}

    // ── Info del pago ─────────────────────────────────────────────────────
    @Schema(description = "Información del pago asociado al pedido")
    public record PagoInfo(
            String        metodoPago,
            String        estadoPago,
            BigDecimal    monto,
            LocalDateTime fechaPago
    ) {}

    // ── Constructor estático desde el dominio ─────────────────────────────
    public static PedidoResponse from(Pedido p) {

        List<DetalleResponse> detalles = p.getDetalles() != null
                ? p.getDetalles().stream()
                        .map(d -> new DetalleResponse(
                                d.getIdProducto(),
                                d.getNombreProducto(),
                                d.getImagenUrl(),
                                d.getCantidad(),
                                d.getPrecioUnitario(),
                                d.getSubtotal()))
                        .toList()
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
                pago,
                p.getNombreCliente(),   // ← NUEVO
                p.getEmailCliente()     // ← NUEVO
        );
    }
}