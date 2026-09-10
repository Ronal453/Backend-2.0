package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.ReporteVentas;
import com.plantopolis.backend.domain.port.out.ProductoRepositoryPort;
import com.plantopolis.backend.infrastructure.persistence.repository.DetallePedidoJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.repository.PagoJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.repository.PedidoJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para AdminReporteService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminReporteService")
class AdminReporteServiceTest {

    @Mock private PagoJpaRepository          pagoRepo;
    @Mock private PedidoJpaRepository        pedidoRepo;
    @Mock private DetallePedidoJpaRepository detalleRepo;
    @Mock private ProductoRepositoryPort     productoRepository;

    @InjectMocks
    private AdminReporteService sut;

    // ── Helpers para crear Object[] con tipo correcto ─────────────────────

    /** countPorEstado() devuelve List<Object[]> donde [0]=estado [1]=count */
    private List<Object[]> estadoRows(Object[]... rows) {
        return Arrays.asList(rows);
    }

    private Object[] estadoRow(String estado, long count) {
        return new Object[]{estado, count};
    }

    /** findTopProductos() devuelve List<Object[]> donde [0]=id [1]=nombre [2]=vendido [3]=ingresos */
    private List<Object[]> topRows(Object[]... rows) {
        return Arrays.asList(rows);
    }

    private Object[] topRow(Long id, String nombre, long vendido, BigDecimal ingresos) {
        return new Object[]{id, nombre, vendido, ingresos};
    }

    private void setupBasico(BigDecimal ingresos, long pedidos, long activos) {
        when(pagoRepo.getTotalIngresosAprobados()).thenReturn(ingresos);
        when(pedidoRepo.count()).thenReturn(pedidos);
        when(productoRepository.contarActivos()).thenReturn(activos);
        when(pedidoRepo.countPorEstado()).thenReturn(new ArrayList<>());
        when(detalleRepo.findTopProductos(any(Pageable.class))).thenReturn(new ArrayList<>());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // KPIs BÁSICOS
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("KPIs básicos")
    class KpisBasicosTests {

        @Test
        @DisplayName("Debería retornar los ingresos totales correctamente")
        void obtenerReporte_retornaTotalIngresos() {
            setupBasico(new BigDecimal("1250000"), 42L, 18L);
            ReporteVentas resultado = sut.obtenerReporte();
            assertThat(resultado.getTotalIngresos()).isEqualByComparingTo("1250000");
        }

        @Test
        @DisplayName("Debería retornar el total de pedidos correctamente")
        void obtenerReporte_retornaTotalPedidos() {
            setupBasico(new BigDecimal("500000"), 15L, 10L);
            ReporteVentas resultado = sut.obtenerReporte();
            assertThat(resultado.getTotalPedidos()).isEqualTo(15L);
        }

        @Test
        @DisplayName("Debería retornar el total de productos activos")
        void obtenerReporte_retornaTotalProductosActivos() {
            setupBasico(BigDecimal.ZERO, 0L, 25L);
            ReporteVentas resultado = sut.obtenerReporte();
            assertThat(resultado.getTotalProductosActivos()).isEqualTo(25L);
        }

        @Test
        @DisplayName("Debería calcular el promedio por orden correctamente")
        void obtenerReporte_calculaPromedioOrden() {
            // 100000 / 4 = 25000
            setupBasico(new BigDecimal("100000"), 4L, 10L);
            ReporteVentas resultado = sut.obtenerReporte();
            assertThat(resultado.getPromedioOrden()).isEqualByComparingTo("25000.00");
        }

        @Test
        @DisplayName("Debería retornar promedio 0 cuando no hay pedidos (evita división por cero)")
        void obtenerReporte_sinPedidos_promedioEsCero() {
            setupBasico(BigDecimal.ZERO, 0L, 5L);
            ReporteVentas resultado = sut.obtenerReporte();
            assertThat(resultado.getPromedioOrden()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Debería manejar ingresos null del repositorio como cero")
        void obtenerReporte_ingresosNull_usaCero() {
            when(pagoRepo.getTotalIngresosAprobados()).thenReturn(null);
            when(pedidoRepo.count()).thenReturn(0L);
            when(productoRepository.contarActivos()).thenReturn(0L);
            when(pedidoRepo.countPorEstado()).thenReturn(new ArrayList<>());
            when(detalleRepo.findTopProductos(any())).thenReturn(new ArrayList<>());

            ReporteVentas resultado = sut.obtenerReporte();
            assertThat(resultado.getTotalIngresos()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Debería manejar contarActivos null del repositorio como cero")
        void obtenerReporte_activosNull_usaCero() {
            when(pagoRepo.getTotalIngresosAprobados()).thenReturn(BigDecimal.ZERO);
            when(pedidoRepo.count()).thenReturn(0L);
            when(productoRepository.contarActivos()).thenReturn(null);
            when(pedidoRepo.countPorEstado()).thenReturn(new ArrayList<>());
            when(detalleRepo.findTopProductos(any())).thenReturn(new ArrayList<>());

            ReporteVentas resultado = sut.obtenerReporte();
            assertThat(resultado.getTotalProductosActivos()).isZero();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ESTADOS POR PEDIDO
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("pedidosPorEstado")
    class PedidosPorEstadoTests {

        @Test
        @DisplayName("Debería incluir los 5 estados aunque todos sean cero")
        void obtenerReporte_incluyeLosCincoEstados() {
            setupBasico(BigDecimal.ZERO, 0L, 0L);
            ReporteVentas resultado = sut.obtenerReporte();
            assertThat(resultado.getPedidosPorEstado())
                    .containsKeys("PENDIENTE", "PREPARANDO", "ENVIADO", "ENTREGADO", "CANCELADO");
        }

        @Test
        @DisplayName("Debería poblar correctamente los conteos reales de la BD")
        void obtenerReporte_poblaPorEstadoDesdeDB() {
            when(pagoRepo.getTotalIngresosAprobados()).thenReturn(BigDecimal.ZERO);
            when(pedidoRepo.count()).thenReturn(18L);
            when(productoRepository.contarActivos()).thenReturn(10L);
            when(detalleRepo.findTopProductos(any())).thenReturn(new ArrayList<>());
            when(pedidoRepo.countPorEstado()).thenReturn(estadoRows(
                    estadoRow("PENDIENTE",  5L),
                    estadoRow("PREPARANDO", 3L),
                    estadoRow("ENVIADO",    2L),
                    estadoRow("ENTREGADO", 10L),
                    estadoRow("CANCELADO",  1L)
            ));

            ReporteVentas resultado = sut.obtenerReporte();

            assertThat(resultado.getPedidosPorEstado())
                    .containsEntry("PENDIENTE",  5L)
                    .containsEntry("PREPARANDO", 3L)
                    .containsEntry("ENVIADO",    2L)
                    .containsEntry("ENTREGADO", 10L)
                    .containsEntry("CANCELADO",  1L);
        }

        @Test
        @DisplayName("Debería mantener en 0 los estados no devueltos por la BD")
        void obtenerReporte_estadosSinDatos_quedan0() {
            when(pagoRepo.getTotalIngresosAprobados()).thenReturn(BigDecimal.ZERO);
            when(pedidoRepo.count()).thenReturn(3L);
            when(productoRepository.contarActivos()).thenReturn(0L);
            when(detalleRepo.findTopProductos(any())).thenReturn(new ArrayList<>());
            // Solo PENDIENTE tiene datos
            when(pedidoRepo.countPorEstado()).thenReturn(
                    estadoRows(estadoRow("PENDIENTE", 3L)));

            ReporteVentas resultado = sut.obtenerReporte();

            assertThat(resultado.getPedidosPorEstado())
                    .containsEntry("PENDIENTE",  3L)
                    .containsEntry("PREPARANDO", 0L)
                    .containsEntry("ENVIADO",    0L)
                    .containsEntry("ENTREGADO",  0L)
                    .containsEntry("CANCELADO",  0L);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TOP PRODUCTOS
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("topProductos")
    class TopProductosTests {

        @Test
        @DisplayName("Debería retornar los top 5 productos más vendidos")
        void obtenerReporte_retornaTopProductos() {
            when(pagoRepo.getTotalIngresosAprobados()).thenReturn(BigDecimal.ZERO);
            when(pedidoRepo.count()).thenReturn(0L);
            when(productoRepository.contarActivos()).thenReturn(0L);
            when(pedidoRepo.countPorEstado()).thenReturn(new ArrayList<>());
            when(detalleRepo.findTopProductos(any())).thenReturn(topRows(
                    topRow(1L, "Pothos Dorado",  48L, new BigDecimal("1200000")),
                    topRow(2L, "Monstera",       35L, new BigDecimal("3150000")),
                    topRow(3L, "Sansevieria",    28L, new BigDecimal("980000")),
                    topRow(4L, "Ficus Lyrata",   20L, new BigDecimal("1600000")),
                    topRow(5L, "Suculenta Mix",  15L, new BigDecimal("225000"))
            ));

            ReporteVentas resultado = sut.obtenerReporte();

            assertThat(resultado.getTopProductos()).hasSize(5);
            assertThat(resultado.getTopProductos().get(0).getNombreProducto())
                    .isEqualTo("Pothos Dorado");
            assertThat(resultado.getTopProductos().get(0).getTotalVendido()).isEqualTo(48L);
        }

        @Test
        @DisplayName("Debería pedir exactamente los top 5 al repositorio")
        void obtenerReporte_pedirTop5AlRepositorio() {
            setupBasico(BigDecimal.ZERO, 0L, 0L);
            sut.obtenerReporte();
            verify(detalleRepo).findTopProductos(PageRequest.of(0, 5));
        }

        @Test
        @DisplayName("Debería retornar lista vacía de topProductos cuando no hay ventas")
        void obtenerReporte_sinVentas_topProductosVacio() {
            setupBasico(BigDecimal.ZERO, 0L, 0L);
            ReporteVentas resultado = sut.obtenerReporte();
            assertThat(resultado.getTopProductos()).isEmpty();
        }

        @Test
        @DisplayName("Debería mapear correctamente id, nombre, vendido e ingresos")
        void obtenerReporte_mapeaCamposCorrectamente() {
            when(pagoRepo.getTotalIngresosAprobados()).thenReturn(BigDecimal.ZERO);
            when(pedidoRepo.count()).thenReturn(0L);
            when(productoRepository.contarActivos()).thenReturn(0L);
            when(pedidoRepo.countPorEstado()).thenReturn(new ArrayList<>());
            when(detalleRepo.findTopProductos(any())).thenReturn(
                    topRows(topRow(7L, "Calathea", 12L, new BigDecimal("600000"))));

            ReporteVentas resultado = sut.obtenerReporte();

            var top = resultado.getTopProductos().get(0);
            assertThat(top.getIdProducto()).isEqualTo(7L);
            assertThat(top.getNombreProducto()).isEqualTo("Calathea");
            assertThat(top.getTotalVendido()).isEqualTo(12L);
            assertThat(top.getTotalIngresos()).isEqualByComparingTo("600000");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LLAMADAS AL REPOSITORIO
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Interacciones con repositorios")
    class InteraccionesTests {

        @Test
        @DisplayName("Debería consultar todos los repositorios exactamente una vez")
        void obtenerReporte_consultaCadaRepositorioUnaVez() {
            setupBasico(new BigDecimal("50000"), 2L, 8L);
            sut.obtenerReporte();
            verify(pagoRepo,              times(1)).getTotalIngresosAprobados();
            verify(pedidoRepo,            times(1)).count();
            verify(pedidoRepo,            times(1)).countPorEstado();
            verify(productoRepository,    times(1)).contarActivos();
            verify(detalleRepo,           times(1)).findTopProductos(any());
        }

        @Test
        @DisplayName("Debería retornar un objeto ReporteVentas no nulo siempre")
        void obtenerReporte_retornaObjetoNoNulo() {
            setupBasico(BigDecimal.ZERO, 0L, 0L);
            ReporteVentas resultado = sut.obtenerReporte();
            assertThat(resultado).isNotNull();
            assertThat(resultado.getPedidosPorEstado()).isNotNull();
            assertThat(resultado.getTopProductos()).isNotNull();
        }
    }
}
