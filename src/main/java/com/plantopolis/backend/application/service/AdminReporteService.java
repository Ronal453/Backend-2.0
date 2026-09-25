package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.Pedido;
import com.plantopolis.backend.domain.model.ProductoMasVendido;
import com.plantopolis.backend.domain.model.ReporteVentas;
import com.plantopolis.backend.domain.port.in.ObtenerReportesUseCase;
import com.plantopolis.backend.domain.port.out.PedidoRepositoryPort;
import com.plantopolis.backend.domain.port.out.ProductoRepositoryPort;
import com.plantopolis.backend.domain.port.out.ReporteRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminReporteService implements ObtenerReportesUseCase {

    private final ReporteRepositoryPort      reporteRepository;
    private final ProductoRepositoryPort     productoRepository;
    private final PedidoRepositoryPort       pedidoRepositoryPort;

    private static final DateTimeFormatter FORMATO_FECHA_CSV =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public ReporteVentas obtenerReporte() {
        log.debug("Calculando reporte de ventas...");

        BigDecimal totalIngresos = reporteRepository.obtenerTotalIngresosAprobados();
        if (totalIngresos == null) totalIngresos = BigDecimal.ZERO;

        long totalPedidos = reporteRepository.contarTotalPedidos();

        Long totalProductosActivos = productoRepository.contarActivos();
        if (totalProductosActivos == null) totalProductosActivos = 0L;

        BigDecimal promedioOrden = BigDecimal.ZERO;
        if (totalPedidos > 0) {
            promedioOrden = totalIngresos.divide(BigDecimal.valueOf(totalPedidos), 2, RoundingMode.HALF_UP);
        }

        Map<String, Long> pedidosPorEstado = new LinkedHashMap<>();
        pedidosPorEstado.put("PENDIENTE",  0L);
        pedidosPorEstado.put("PREPARANDO", 0L);
        pedidosPorEstado.put("ENVIADO",    0L);
        pedidosPorEstado.put("ENTREGADO",  0L);
        pedidosPorEstado.put("CANCELADO",  0L);

        // Combinar con los valores reales obtenidos desde el puerto
        Map<String, Long> dbEstados = reporteRepository.contarPedidosPorEstado();
        for (Map.Entry<String, Long> entry : dbEstados.entrySet()) {
            pedidosPorEstado.put(entry.getKey(), entry.getValue());
        }

        List<ProductoMasVendido> topProductos = reporteRepository.obtenerTopProductosVendidos(5);

        return ReporteVentas.builder()
                .totalIngresos(totalIngresos)
                .totalPedidos(totalPedidos)
                .totalProductosActivos(totalProductosActivos)
                .promedioOrden(promedioOrden)
                .pedidosPorEstado(pedidosPorEstado)
                .topProductos(topProductos)
                .build();
    }

    @Override
    public byte[] exportarPedidosCsv(LocalDate fechaInicio, LocalDate fechaFin) {
        var inicio = fechaInicio != null ? fechaInicio.atStartOfDay() : null;
        var fin    = fechaFin    != null ? fechaFin.atTime(LocalTime.MAX) : null;

        List<Pedido> pedidos = pedidoRepositoryPort.buscarPorRangoFecha(inicio, fin);

        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF'); // BOM UTF-8 para que Excel muestre bien tildes/ñ
        csv.append("Numero de pedido,Fecha,Cliente,Correo,Estado,Total\n");

        for (Pedido p : pedidos) {
            csv.append(csvEscape(p.getNumeroPedido())).append(',')
               .append(p.getFechaPedido() != null ? p.getFechaPedido().format(FORMATO_FECHA_CSV) : "")
               .append(',')
               .append(csvEscape(p.getNombreCliente())).append(',')
               .append(csvEscape(p.getEmailCliente())).append(',')
               .append(csvEscape(p.getEstadoDescripcion())).append(',')
               .append(p.getTotal() != null ? p.getTotal().toPlainString() : "0")
               .append('\n');
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String csvEscape(String valor) {
        if (valor == null) return "";
        if (valor.contains(",") || valor.contains("\"") || valor.contains("\n")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }
}