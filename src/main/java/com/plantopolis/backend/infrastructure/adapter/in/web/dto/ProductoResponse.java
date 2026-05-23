package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import com.plantopolis.backend.domain.model.Producto;
import java.math.BigDecimal;

/**
 * DTO de respuesta para un producto.
 *
 * FIX Sprint 5 — Inventario admin:
 *   Se agrega el campo `activo` que estaba ausente.
 *
 *   SIN el campo activo:
 *     - p.activo en el frontend era `undefined` (falsy)
 *     - Todos los productos mostraban badge "Inactivo"
 *     - El toggle siempre llamaba desactivarProducto()
 *       porque: undefined ?? true = true → siempre desactivaba
 *
 *   CON el campo activo:
 *     - p.activo llega como true o false desde el backend
 *     - El badge muestra "Activo" o "Inactivo" correctamente
 *     - El toggle funciona: true → desactivar | false → activar
 */

public record ProductoResponse(
        Long      idProducto,
        String    nombreProducto,
        String    descripcion,
        BigDecimal precio,
        Integer   stock,
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
    /**
     * Constructor estático desde el modelo de dominio.
     * Incluye activo para que el panel admin pueda mostrar
     * y controlar el estado de visibilidad del producto.
     */
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
                p.getNombreTipo(),
                p.getActivo(),   
                p.getIdCategoria(),  
                p.getIdTipo()        
        );
    }
    
}