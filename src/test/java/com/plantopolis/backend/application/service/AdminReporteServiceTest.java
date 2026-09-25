package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.ProductoMasVendido;
import com.plantopolis.backend.domain.model.ReporteVentas;
import com.plantopolis.backend.domain.port.out.PedidoRepositoryPort;
import com.plantopolis.backend.domain.port.out.ProductoRepositoryPort;
import com.plantopolis.backend.domain.port.out.ReporteRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminReporteService")
class AdminReporteServiceTest {

    @Mock private ReporteRepositoryPort reporteRepository;
    @Mock private ProductoRepositoryPort productoRepository;
    @Mock private PedidoRepositoryPort pedidoRepositoryPort;

    @InjectMocks
    private AdminReporteService sut;

    private void setupBasico(BigDecimal ingresos, long pedidos, long activos) {
        when(reporteRepository.obtenerTotalIngresosAprobados()).thenReturn(ingresos);
        when(reporteRepository.contarTotalPedidos()).thenReturn(pedidos);
        when(productoRepository.contarActivos()).thenReturn(activos);
        when(reporteRepository.contarPedidosPorEstado()).thenReturn(new HashMap<>());
        when(reporteRepository.obtenerTopProductosVendidos(anyInt())).thenReturn(new ArrayList<>());
    }

    @Test
    void obtenerReporte_retornaTotalIngresos() {
        setupBasico(new BigDecimal("1250000"), 42L, 18L);
        ReporteVentas resultado = sut.obtenerReporte();
        assertThat(resultado.getTotalIngresos()).isEqualByComparingTo("1250000");
    }

    @Test
    void obtenerReporte_retornaTotalPedidos() {
        setupBasico(new BigDecimal("500000"), 15L, 10L);
        ReporteVentas resultado = sut.obtenerReporte();
        assertThat(resultado.getTotalPedidos()).isEqualTo(15L);
    }

    @Test
    void obtenerReporte_poblaPorEstadoDesdeDB() {
        setupBasico(new BigDecimal("500000"), 15L, 10L);
        Map<String, Long> mockEstados = new HashMap<>();
        mockEstados.put("PENDIENTE", 5L);
        mockEstados.put("ENVIADO", 10L);
        when(reporteRepository.contarPedidosPorEstado()).thenReturn(mockEstados);

        ReporteVentas resultado = sut.obtenerReporte();
        assertThat(resultado.getPedidosPorEstado().get("PENDIENTE")).isEqualTo(5L);
        assertThat(resultado.getPedidosPorEstado().get("ENVIADO")).isEqualTo(10L);
    }

    @Test
    void obtenerReporte_retornaTopProductos() {
        setupBasico(new BigDecimal("500000"), 15L, 10L);
        List<ProductoMasVendido> top = new ArrayList<>();
        top.add(ProductoMasVendido.builder().idProducto(1L).nombreProducto("Ficus").totalVendido(100L).totalIngresos(new BigDecimal("1000")).build());
        when(reporteRepository.obtenerTopProductosVendidos(5)).thenReturn(top);

        ReporteVentas resultado = sut.obtenerReporte();
        assertThat(resultado.getTopProductos()).hasSize(1);
        assertThat(resultado.getTopProductos().get(0).getNombreProducto()).isEqualTo("Ficus");
    }
}
