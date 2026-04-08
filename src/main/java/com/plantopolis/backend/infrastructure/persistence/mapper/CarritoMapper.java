package com.plantopolis.backend.infrastructure.persistence.mapper;

import com.plantopolis.backend.domain.model.Carrito;
import com.plantopolis.backend.domain.model.ItemCarrito;
import com.plantopolis.backend.infrastructure.persistence.entity.CarritoEntity;
import com.plantopolis.backend.infrastructure.persistence.entity.CarritoItemEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Component
public class CarritoMapper {

    public ItemCarrito itemToDomain(CarritoItemEntity entity) {
        if (entity == null) return null;

        BigDecimal precio = entity.getProducto() != null
                ? entity.getProducto().getPrecio()
                : BigDecimal.ZERO;

        BigDecimal subtotal = precio.multiply(
                BigDecimal.valueOf(entity.getCantidad()));

        return ItemCarrito.builder()
                .idItem(entity.getIdItem())
                .idCarrito(entity.getIdCarrito())
                .idProducto(entity.getIdProducto())
                .cantidad(entity.getCantidad())
                .precioUnitario(precio)
                .subtotal(subtotal)
                .nombreProducto(entity.getProducto() != null
                        ? entity.getProducto().getNombreProducto() : null)
                .imagenUrl(entity.getProducto() != null
                        ? entity.getProducto().getImagenUrl() : null)
                .build();
    }

    public Carrito toDomain(CarritoEntity entity) {
        if (entity == null) return null;

        List<ItemCarrito> items = entity.getItems() != null
                ? entity.getItems().stream()
                        .map(this::itemToDomain)
                        .toList()
                : Collections.emptyList();

        // Calcular total
        BigDecimal total = items.stream()
                .map(ItemCarrito::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Carrito.builder()
                .idCarrito(entity.getIdCarrito())
                .idUsuario(entity.getIdUsuario())
                .idEstadoCarrito(entity.getIdEstadoCarrito())
                .estadoDescripcion(entity.getEstado() != null
                        ? entity.getEstado().getDescripcion() : null)
                .fechaCreacion(entity.getFechaCreacion())
                .items(items)
                .total(total)
                .build();
    }
}