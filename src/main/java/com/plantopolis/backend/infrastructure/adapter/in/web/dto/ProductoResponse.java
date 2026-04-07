package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import com.plantopolis.backend.domain.model.Producto;
import java.math.BigDecimal;

public record ProductoResponse(
        Long idProducto,
        String nombreProducto,
        String descripcion,
        BigDecimal precio,
        Integer stock,
        String imagenUrl,
        String cuidados,
        String luz,
        String riego,
        String tamanioEstimado,
        String categoria,
        String tipo
) {
    // Constructor desde dominio
    public static ProductoResponse from(Producto p) {
        return new ProductoResponse(
                p.getIdProducto(),
                p.getNombreProducto(),
                p.getDescripcion(),
                p.getPrecio(),
                p.getStock(),
                p.getImagenUrl(),
                p.getCuidados(),
                p.getLuz(),
                p.getRiego(),
                p.getTamanioEstimado(),
                p.getNombreCategoria(),
                p.getNombreTipo()
        );
    }
}
