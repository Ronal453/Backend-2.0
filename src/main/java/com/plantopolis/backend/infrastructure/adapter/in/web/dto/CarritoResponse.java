package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import com.plantopolis.backend.domain.model.Carrito;
import com.plantopolis.backend.domain.model.ItemCarrito;

import java.math.BigDecimal;
import java.util.List;

public record CarritoResponse(
        Long idCarrito,
        String estado,
        List<ItemResponse> items,
        int totalItems,
        BigDecimal total
) {
    public record ItemResponse(
            Long idItem,
            Long idProducto,
            String nombreProducto,
            String imagenUrl,
            Integer cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal
    ) {
        public static ItemResponse from(ItemCarrito item) {
            return new ItemResponse(
                    item.getIdItem(),
                    item.getIdProducto(),
                    item.getNombreProducto(),
                    item.getImagenUrl(),
                    item.getCantidad(),
                    item.getPrecioUnitario(),
                    item.getSubtotal()
            );
        }
    }

    public static CarritoResponse from(Carrito carrito) {
        List<ItemResponse> items = carrito.getItems() != null
                ? carrito.getItems().stream()
                        .map(ItemResponse::from)
                        .toList()
                : List.of();

        return new CarritoResponse(
                carrito.getIdCarrito(),
                carrito.getEstadoDescripcion(),
                items,
                items.size(),
                carrito.getTotal()
        );
    }
}