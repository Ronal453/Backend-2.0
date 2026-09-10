package com.plantopolis.backend.infrastructure.persistence.mapper;

import com.plantopolis.backend.domain.model.DetallePedido;
import com.plantopolis.backend.domain.model.Pago;
import com.plantopolis.backend.domain.model.Pedido;
import com.plantopolis.backend.infrastructure.persistence.entity.DetallePedidoEntity;
import com.plantopolis.backend.infrastructure.persistence.entity.PagoEntity;
import com.plantopolis.backend.infrastructure.persistence.entity.PedidoEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class PedidoMapper {

    // ── DetallePedido ────────────────────────────────────────
    public DetallePedido detalleToDomain(DetallePedidoEntity e) {
        if (e == null) return null;

        // SUBTOTAL ya es columna persistida,
        // usamos e.getSubtotal() en vez de recalcular precio*cantidad.
        return DetallePedido.builder()
                .idDetalle(e.getIdDetalle())
                .idPedido(e.getIdPedido())
                .idProducto(e.getIdProducto())
                .cantidad(e.getCantidad())
                .precioUnitario(e.getPrecioUnitario())
                .subtotal(e.getSubtotal())
                .nombreProducto(e.getProducto() != null
                        ? e.getProducto().getNombreProducto() : null)
                .imagenUrl(e.getProducto() != null
                        ? e.getProducto().getImagenUrl() : null)
                .build();
    }

    // ── Pago ─────────────────────────────────────────────────
    public Pago pagToDomain(PagoEntity e) {
        if (e == null) return null;

        return Pago.builder()
                .idPago(e.getIdPago())
                .idPedido(e.getIdPedido())
                .idMetodo(e.getIdMetodo())
                .idEstadoPago(e.getIdEstadoPago())
                .monto(e.getMonto())
                .fechaPago(e.getFechaPago())
                .nombreMetodo(e.getMetodo() != null
                        ? e.getMetodo().getNombreMetodo() : null)
                .estadoPagoDescripcion(e.getEstadoPago() != null
                        ? e.getEstadoPago().getDescripcionEstadoPago() : null)
                .build();
    }

    // ── Pedido ───────────────────────────────────────────────
    public Pedido toDomain(PedidoEntity e) {
        if (e == null) return null;

        List<DetallePedido> detalles = e.getDetalles() != null
                ? e.getDetalles().stream()
                        .map(this::detalleToDomain)
                        .toList()
                : Collections.emptyList();

        // impuestos y total ya son columnas
        // persistidas en PEDIDO (antes se recalculaba sumando detalles,
        // ignorando impuestos).
        return Pedido.builder()
                .idPedido(e.getIdPedido())
                .idUsuario(e.getIdUsuario())
                .idEstado(e.getIdEstado())
                .fechaPedido(e.getFechaPedido())
                .direccionEnvio(e.getDireccionEnvio())
                .numeroPedido(e.getNumeroPedido())
                .subtotal(e.getSubtotal())
                .impuestos(e.getImpuestos())
                .total(e.getTotal())
                .estadoDescripcion(e.getEstado() != null
                        ? e.getEstado().getDescripcionEstado() : null)
                .emailCliente(e.getUsuario() != null
                        ? e.getUsuario().getCorreo() : null)
                .nombreCliente(e.getUsuario() != null
                        ? e.getUsuario().getNombreCompleto() : null)
                .detalles(detalles)
                .pago(pagToDomain(e.getPago()))
                .build();
    }
}