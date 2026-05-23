package com.plantopolis.backend.domain.model;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Modelo de dominio para el reporte general de ventas.
 * Agrupa todas las métricas que se muestran en el dashboard admin:
 *   - Ingresos totales
 *   - Total de pedidos y productos
 *   - Distribución de pedidos por estado
 *   - Top 5 productos más vendidos
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteVentas {

    // Suma de todos los pagos APROBADOS (estado id=2)
    private BigDecimal totalIngresos;

    // Cantidad total de pedidos en el sistema
    private Long totalPedidos;

    // Cantidad de productos activos en el catálogo
    private Long totalProductosActivos;

    // Valor promedio de cada pedido
    private BigDecimal promedioOrden;

    // Mapa: { "PENDIENTE": 10, "ENVIADO": 5, "ENTREGADO": 3, ... }
    // Permite construir gráficas de barras en el frontend
    private Map<String, Long> pedidosPorEstado;

    // Top 5 productos más vendidos (por unidades)
    private List<ProductoMasVendido> topProductos;
}