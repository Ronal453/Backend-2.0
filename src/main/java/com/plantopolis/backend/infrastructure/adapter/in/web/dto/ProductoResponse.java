package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import com.plantopolis.backend.domain.model.Producto;
import java.math.BigDecimal;

public record ProductoResponse(
        Long      idProducto,
        String    nombreProducto,
        String    descripcion,
        BigDecimal precio,
        Integer   stock,
        Integer   stockMinimoAlerta,
        Boolean   stockCritico,
        String    imagenUrl,
        String    cuidados,
        String    luz,
        String    riego,
        String    tamanioEstimado,
        String    categoria,
        String    tipo,
        Boolean   activo,
        Long      idCategoria,
        Long      idTipo
) {
    public static ProductoResponse from(Producto p) {
        boolean critico = p.getStock() != null
                && p.getStockMinimoAlerta() != null
                && p.getStock() <= p.getStockMinimoAlerta();

        return new ProductoResponse(
                p.getIdProducto(), p.getNombreProducto(), p.getDescripcion(),
                p.getPrecio(), p.getStock(), p.getStockMinimoAlerta(), critico,
                p.getImagenUrl(), p.getCuidados(), p.getLuz(), p.getRiego(),
                p.getTamanioEstimado(), p.getNombreCategoria(), p.getNombreTipo(),
                p.getActivo(), p.getIdCategoria(), p.getIdTipo()
        );
    }
}