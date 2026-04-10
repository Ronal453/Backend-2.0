package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import com.plantopolis.backend.domain.model.Pedido;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
        Long idPedido,
        String numeroPedido,
        String estado,
        LocalDateTime fechaPedido,
        String direccionEnvio,
        BigDecimal total,
        List<DetalleResponse> detalles,
        PagoInfo pago
) {
    public record DetalleResponse(
            Long idProducto,
            String nombreProducto,
            String imagenUrl,
            Integer cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal
    ) {}

    public record PagoInfo(
            String metodoPago,
            String estadoPago,
            BigDecimal monto,
            LocalDateTime fechaPago
    ) {}

    public static PedidoResponse from(Pedido p) {
        List<DetalleResponse> detalles = p.getDetalles() != null
                ? p.getDetalles().stream().map(d -> new DetalleResponse(
                        d.getIdProducto(), d.getNombreProducto(),
                        d.getImagenUrl(), d.getCantidad(),
                        d.getPrecioUnitario(), d.getSubtotal()
                  )).toList()
                : List.of();

        PagoInfo pago = p.getPago() != null
                ? new PagoInfo(p.getPago().getNombreMetodo(),
                               p.getPago().getEstadoPagoDescripcion(),
                               p.getPago().getMonto(),
                               p.getPago().getFechaPago())
                : null;

        return new PedidoResponse(
                p.getIdPedido(), p.getNumeroPedido(),
                p.getEstadoDescripcion(), p.getFechaPedido(),
                p.getDireccionEnvio(), p.getTotal(),
                detalles, pago
        );
    }
}
