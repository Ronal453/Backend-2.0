package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import com.plantopolis.backend.domain.model.ProductoMasVendido;
import com.plantopolis.backend.domain.model.ReporteVentas;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DTO de respuesta para el dashboard de reportes del panel admin.
 *
 * Se usa en: GET /api/admin/reportes
 *
 * Contiene todas las métricas necesarias para el frontend:
 *   - KPI cards (ingresos, pedidos, productos, promedio)
 *   - Gráfica de barras (pedidosPorEstado)
 *   - Tabla de top productos (topProductos)
 *
 */
@Schema(description = "Métricas del dashboard de administración")
public record ReporteResponse(

        @Schema(description = "Suma de todos los pagos aprobados",
                example = "1250000.00")
        BigDecimal totalIngresos,

        @Schema(description = "Total de pedidos en el sistema", example = "42")
        Long totalPedidos,

        @Schema(description = "Productos activos en el catálogo", example = "18")
        Long totalProductosActivos,

        @Schema(description = "Valor promedio de cada pedido", example = "29761.90")
        BigDecimal promedioOrden,

        @Schema(description = "Cantidad de pedidos agrupados por estado. " +
                              "Siempre incluye los 5 estados aunque sean 0.",
                example = "{\"PENDIENTE\":5,\"PREPARANDO\":3,\"ENVIADO\":2," +
                          "\"ENTREGADO\":10,\"CANCELADO\":1}")
        Map<String, Long> pedidosPorEstado,

        @Schema(description = "Top 5 productos más vendidos por unidades")
        List<TopProductoDTO> topProductos

) {

    // ── DTO interno: producto del ranking ─────────────────────────────────
    @Schema(description = "Producto con sus métricas de venta")
    public record TopProductoDTO(

            @Schema(description = "ID del producto", example = "3")
            Long idProducto,

            @Schema(description = "Nombre del producto",
                    example = "Pothos Dorado")
            String nombreProducto,

            @Schema(description = "Unidades totales vendidas", example = "48")
            Long totalVendido,

            @Schema(description = "Ingresos generados por este producto",
                    example = "1200000.00")
            BigDecimal totalIngresos
    ) {
        // Constructor estático desde el modelo de dominio
        public static TopProductoDTO from(ProductoMasVendido p) {
            return new TopProductoDTO(
                    p.getIdProducto(),
                    p.getNombreProducto(),
                    p.getTotalVendido(),
                    p.getTotalIngresos()
            );
        }
    }

    // ── Constructor estático desde el modelo de dominio ───────────────────
    /**
     * Convierte un ReporteVentas del dominio en un ReporteResponse DTO.
     * Se llama desde el controller: ReporteResponse.from(reporte)
     */
    public static ReporteResponse from(ReporteVentas reporte) {
        List<TopProductoDTO> top = reporte.getTopProductos() != null
                ? reporte.getTopProductos().stream()
                        .map(TopProductoDTO::from)
                        .toList()
                : List.of();

        return new ReporteResponse(
                reporte.getTotalIngresos(),
                reporte.getTotalPedidos(),
                reporte.getTotalProductosActivos(),
                reporte.getPromedioOrden(),
                reporte.getPedidosPorEstado(),
                top
        );
    }
}