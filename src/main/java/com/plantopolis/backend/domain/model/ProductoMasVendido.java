package com.plantopolis.backend.domain.model;

import lombok.*;
import java.math.BigDecimal;

/**
 * Modelo de dominio para el reporte de productos más vendidos.
 * Se usa en el dashboard del administrador para mostrar
 * qué productos generan más ventas e ingresos.
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoMasVendido {

    // ID del producto en la BD
    private Long idProducto;

    // Nombre del producto (enriquecido desde la relación JPA)
    private String nombreProducto;

    // Total de unidades vendidas (SUM de DetallePedido.cantidad)
    private Long totalVendido;

    // Total de ingresos generados (SUM de precioUnitario * cantidad)
    private BigDecimal totalIngresos;
}