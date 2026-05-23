package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.ProductoMasVendido;
import com.plantopolis.backend.domain.model.ReporteVentas;
import com.plantopolis.backend.domain.port.in.ObtenerReportesUseCase;
import com.plantopolis.backend.domain.port.out.ProductoRepositoryPort;
import com.plantopolis.backend.infrastructure.persistence.repository.DetallePedidoJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.repository.PagoJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.repository.PedidoJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de aplicación para el dashboard de reportes de ventas.
 * Agrega métricas en tiempo real desde múltiples repositorios:
 *   - PagoJpaRepository       → total ingresos aprobados
 *   - PedidoJpaRepository     → conteo por estado
 *   - DetallePedidoJpaRepository → top productos más vendidos
 *   - ProductoRepositoryPort  → total productos activos
 *
 * Nota: inyectamos repositorios JPA directamente porque las queries
 * de agregación son específicas de la infraestructura y no tienen sentido
 * en el dominio puro. Patrón consistente con PedidoService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminReporteService implements ObtenerReportesUseCase {

    // Repositorio de pagos: calcula el total de ingresos aprobados
    private final PagoJpaRepository          pagoRepo;

    // Repositorio de pedidos: cuenta por estado para la gráfica
    private final PedidoJpaRepository        pedidoRepo;

    // Repositorio de detalles: calcula el top de productos vendidos
    private final DetallePedidoJpaRepository detalleRepo;

    // Puerto de dominio: cuenta productos activos
    private final ProductoRepositoryPort     productoRepository;

    /**
     * Calcula y devuelve todas las métricas del dashboard en tiempo real.
     *
     * Cada métrica se calcula con una query SQL independiente para evitar
     * joins complejos. Spring los ejecuta en el mismo request.
     *
     * @return ReporteVentas con todas las métricas agregadas
     */
    @Override
    public ReporteVentas obtenerReporte() {
        log.debug("Calculando reporte de ventas...");

        // ── 1. Total ingresos (pagos aprobados) ───────────────────────────
        // Suma el monto de todos los pagos con idEstadoPago = 2 (APROBADO)
        // COALESCE evita null si no hay pagos aún
        BigDecimal totalIngresos = pagoRepo.getTotalIngresosAprobados();
        if (totalIngresos == null) totalIngresos = BigDecimal.ZERO;

        // ── 2. Total de pedidos en el sistema ─────────────────────────────
        long totalPedidos = pedidoRepo.count();

        // ── 3. Total de productos activos en el catálogo ──────────────────
        Long totalProductosActivos = productoRepository.contarActivos();
        if (totalProductosActivos == null) totalProductosActivos = 0L;

        // ── 4. Promedio por orden ─────────────────────────────────────────
        // totalIngresos / totalPedidos (evita división por cero)
        BigDecimal promedioOrden = BigDecimal.ZERO;
        if (totalPedidos > 0) {
            promedioOrden = totalIngresos
                    .divide(BigDecimal.valueOf(totalPedidos), 2, RoundingMode.HALF_UP);
        }

        // ── 5. Distribución de pedidos por estado ─────────────────────────
        // Query: SELECT e.descripcionEstado, COUNT(p) GROUP BY e.descripcionEstado
        // Resultado: List<Object[]> = [[estado, count], [estado, count], ...]
        // Usamos LinkedHashMap para preservar el orden de inserción
        Map<String, Long> pedidosPorEstado = new LinkedHashMap<>();

        // Pre-poblar todos los estados en orden lógico del flujo
        // Así el frontend siempre recibe los 5 estados (aunque alguno tenga 0)
        pedidosPorEstado.put("PENDIENTE",  0L);
        pedidosPorEstado.put("PREPARANDO", 0L);
        pedidosPorEstado.put("ENVIADO",    0L);
        pedidosPorEstado.put("ENTREGADO",  0L);
        pedidosPorEstado.put("CANCELADO",  0L);

        // Llenar con los valores reales de la BD
        pedidoRepo.countPorEstado().forEach(row -> {
            String estado = (String) row[0];   // descripcionEstado
            Long   count  = (Long)   row[1];   // COUNT(p)
            pedidosPorEstado.put(estado, count);
        });

        // ── 6. Top 5 productos más vendidos ──────────────────────────────
        // Query agrupada en DetallePedido, excluyendo pedidos cancelados
        // PageRequest.of(0, 5) limita el resultado a los primeros 5
        List<ProductoMasVendido> topProductos = detalleRepo
                .findTopProductos(PageRequest.of(0, 5))
                .stream()
                .map(row -> ProductoMasVendido.builder()
                        .idProducto((Long) row[0])
                        .nombreProducto((String) row[1])
                        .totalVendido((Long) row[2])
                        .totalIngresos((BigDecimal) row[3])
                        .build())
                .toList();

        log.debug("Reporte calculado: ingresos={}, pedidos={}, activos={}",
                totalIngresos, totalPedidos, totalProductosActivos);

        // ── Construir y retornar el reporte completo ──────────────────────
        return ReporteVentas.builder()
                .totalIngresos(totalIngresos)
                .totalPedidos(totalPedidos)
                .totalProductosActivos(totalProductosActivos)
                .promedioOrden(promedioOrden)
                .pedidosPorEstado(pedidosPorEstado)
                .topProductos(topProductos)
                .build();
    }
}