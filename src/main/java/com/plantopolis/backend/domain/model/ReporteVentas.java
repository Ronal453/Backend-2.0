package com.plantopolis.backend.domain.model;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Modelo de dominio para el reporte general de operaciones y ventas.
 * Agrupa todas las métricas que se muestran en el dashboard admin:
 *   - E-commerce: Ingresos, pedidos, productos, top ventas.
 *   - Operaciones Agrícolas: Lotes, mermas, tareas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteVentas {

    // --- E-Commerce Metrics ---
    // Suma de todos los pagos APROBADOS
    private BigDecimal totalIngresos;

    // Cantidad total de pedidos en el sistema
    private Long totalPedidos;

    // Cantidad de productos activos en el catálogo
    private Long totalProductosActivos;

    // Valor promedio de cada pedido
    private BigDecimal promedioOrden;

    // Mapa: { "PENDIENTE": 10, "ENVIADO": 5, "ENTREGADO": 3, ... }
    private Map<String, Long> pedidosPorEstado;

    // Top 5 productos más vendidos (por unidades)
    private List<ProductoMasVendido> topProductos;

    // --- Agricultural Operations Metrics ---
    // Total de plantas perdidas registradas en mermas
    private Long totalPlantasPerdidas;

    // Mapa de lotes por fase de crecimiento: { "GERMINANDO": 5, "CRECIENDO": 12, "LISTO_PARA_VENTA": 3 }
    private Map<String, Long> lotesPorFase;

    // Mapa de tareas por estado: { "POR_HACER": 10, "EN_PROGRESO": 4, "COMPLETADA": 20, "BLOQUEADA": 1 }
    private Map<String, Long> tareasPorEstado;

    // Mapa de mermas por causa: { "PLAGA": 50, "CLIMA": 20, "ENFERMEDAD": 100 }
    private Map<String, Long> mermasPorCausa;
}