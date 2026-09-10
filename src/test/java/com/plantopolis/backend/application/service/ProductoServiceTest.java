package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.Categoria;
import com.plantopolis.backend.domain.model.Producto;
import com.plantopolis.backend.domain.model.TipoProducto;
import com.plantopolis.backend.domain.port.out.ProductoRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para ProductoService.
 *
 * Cubre:
 *   - listarProductos(): con y sin filtros
 *   - obtenerDetalle(): existente, no encontrado
 *   - listarCategorias(): lista vacía y con datos
 *   - listarTipos(): delegación al repositorio
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductoService")
class ProductoServiceTest {

    @Mock private ProductoRepositoryPort productoRepository;

    @InjectMocks
    private ProductoService sut;

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private Producto producto(Long id, String nombre, BigDecimal precio) {
        return Producto.builder()
                .idProducto(id)
                .nombreProducto(nombre)
                .precio(precio)
                .stock(10)
                .activo(true)
                .nombreCategoria("Plantas de Interior")
                .nombreTipo("Planta")
                .build();
    }

    private Pageable pageable() {
        return PageRequest.of(0, 12);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LISTAR PRODUCTOS
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("listarProductos()")
    class ListarProductosTests {

        @Test
        @DisplayName("Debería retornar página de productos sin filtros")
        void listarProductos_sinFiltros_retornaPagina() {
            // Arrange
            List<Producto> lista = List.of(
                    producto(1L, "Pothos Dorado", new BigDecimal("25000")),
                    producto(2L, "Sansevieria", new BigDecimal("35000"))
            );
            Page<Producto> pagina = new PageImpl<>(lista, pageable(), 2);

            when(productoRepository.buscarConFiltros(
                    isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(pagina);

            // Act
            Page<Producto> resultado = sut.listarProductos(
                    null, null, null, null, null, pageable());

            // Assert
            assertThat(resultado.getContent()).hasSize(2);
            assertThat(resultado.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("Debería filtrar por nombre cuando se proporciona")
        void listarProductos_conFiltroNombre_delegaAlRepositorio() {
            // Arrange
            Page<Producto> pagina = new PageImpl<>(
                    List.of(producto(1L, "Pothos Dorado", new BigDecimal("25000"))));

            when(productoRepository.buscarConFiltros(
                    eq("Pothos"), isNull(), isNull(), isNull(), isNull(), any()))
                    .thenReturn(pagina);

            // Act
            Page<Producto> resultado = sut.listarProductos(
                    "Pothos", null, null, null, null, pageable());

            // Assert
            assertThat(resultado.getContent()).hasSize(1);
            assertThat(resultado.getContent().get(0).getNombreProducto())
                    .isEqualTo("Pothos Dorado");
        }

        @Test
        @DisplayName("Debería filtrar por rango de precios")
        void listarProductos_conFiltroPrecio_delegaAlRepositorio() {
            // Arrange
            BigDecimal min = new BigDecimal("10000");
            BigDecimal max = new BigDecimal("30000");
            Page<Producto> pagina = new PageImpl<>(List.of());

            when(productoRepository.buscarConFiltros(
                    isNull(), isNull(), isNull(), eq(min), eq(max), any()))
                    .thenReturn(pagina);

            // Act
            Page<Producto> resultado = sut.listarProductos(
                    null, null, null, min, max, pageable());

            // Assert
            assertThat(resultado).isNotNull();
            verify(productoRepository).buscarConFiltros(
                    null, null, null, min, max, pageable());
        }

        @Test
        @DisplayName("Debería retornar página vacía cuando no hay resultados")
        void listarProductos_sinResultados_retornaPaginaVacia() {
            // Arrange
            when(productoRepository.buscarConFiltros(
                    any(), any(), any(), any(), any(), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            // Act
            Page<Producto> resultado = sut.listarProductos(
                    "inexistente", null, null, null, null, pageable());

            // Assert
            assertThat(resultado.getContent()).isEmpty();
            assertThat(resultado.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("Debería filtrar combinando categoría y tipo")
        void listarProductos_conFiltrosCategoriaYTipo_delegaCorrectamente() {
            // Arrange
            when(productoRepository.buscarConFiltros(
                    isNull(), eq(1L), eq(2L), isNull(), isNull(), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            // Act
            sut.listarProductos(null, 1L, 2L, null, null, pageable());

            // Assert
            verify(productoRepository).buscarConFiltros(null, 1L, 2L, null, null, pageable());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // OBTENER DETALLE
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("obtenerDetalle()")
    class ObtenerDetalleTests {

        @Test
        @DisplayName("Debería retornar el producto cuando existe")
        void obtenerDetalle_productoExistente_loRetorna() {
            // Arrange
            Producto esperado = producto(1L, "Monstera Deliciosa", new BigDecimal("45000"));
            when(productoRepository.buscarPorId(1L)).thenReturn(Optional.of(esperado));

            // Act
            Producto resultado = sut.obtenerDetalle(1L);

            // Assert
            assertThat(resultado).isNotNull();
            assertThat(resultado.getIdProducto()).isEqualTo(1L);
            assertThat(resultado.getNombreProducto()).isEqualTo("Monstera Deliciosa");
            assertThat(resultado.getPrecio()).isEqualByComparingTo("45000");
        }

        @Test
        @DisplayName("Debería lanzar RuntimeException cuando el producto no existe")
        void obtenerDetalle_productoInexistente_lanzaExcepcion() {
            // Arrange
            when(productoRepository.buscarPorId(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> sut.obtenerDetalle(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Producto no encontrado")
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("Debería retornar el producto con todos sus atributos de cuidado")
        void obtenerDetalle_retornaAtributosCuidado() {
            // Arrange
            Producto conCuidados = Producto.builder()
                    .idProducto(5L)
                    .nombreProducto("Ficus Lyrata")
                    .precio(new BigDecimal("80000"))
                    .stock(5)
                    .activo(true)
                    .luz("Luz indirecta brillante")
                    .riego("Cada 7 días")
                    .cuidados("Limpiar las hojas mensualmente")
                    .tamanioEstimado("60-90 cm")
                    .build();

            when(productoRepository.buscarPorId(5L)).thenReturn(Optional.of(conCuidados));

            // Act
            Producto resultado = sut.obtenerDetalle(5L);

            // Assert
            assertThat(resultado.getLuz()).isEqualTo("Luz indirecta brillante");
            assertThat(resultado.getRiego()).isEqualTo("Cada 7 días");
            assertThat(resultado.getCuidados()).isEqualTo("Limpiar las hojas mensualmente");
            assertThat(resultado.getTamanioEstimado()).isEqualTo("60-90 cm");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LISTAR CATEGORÍAS
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("listarCategorias()")
    class ListarCategoriasTests {

        @Test
        @DisplayName("Debería retornar todas las categorías disponibles")
        void listarCategorias_retornaLista() {
            // Arrange
            List<Categoria> categorias = List.of(
                    Categoria.builder().idCategoria(1L).nombreCategoria("Plantas de Interior").build(),
                    Categoria.builder().idCategoria(2L).nombreCategoria("Suculentas y Cactus").build(),
                    Categoria.builder().idCategoria(3L).nombreCategoria("Aromáticas").build()
            );
            when(productoRepository.listarCategorias()).thenReturn(categorias);

            // Act
            List<Categoria> resultado = sut.listarCategorias();

            // Assert
            assertThat(resultado).hasSize(3);
            assertThat(resultado).extracting(Categoria::getNombreCategoria)
                    .contains("Plantas de Interior", "Suculentas y Cactus", "Aromáticas");
        }

        @Test
        @DisplayName("Debería retornar lista vacía cuando no hay categorías")
        void listarCategorias_sinCategorias_retornaListaVacia() {
            // Arrange
            when(productoRepository.listarCategorias()).thenReturn(List.of());

            // Act
            List<Categoria> resultado = sut.listarCategorias();

            // Assert
            assertThat(resultado).isEmpty();
        }

        @Test
        @DisplayName("Debería delegar al repositorio sin transformación adicional")
        void listarCategorias_delegaAlRepositorio() {
            // Arrange
            when(productoRepository.listarCategorias()).thenReturn(List.of());

            // Act
            sut.listarCategorias();

            // Assert
            verify(productoRepository, times(1)).listarCategorias();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LISTAR TIPOS
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("listarTipos()")
    class ListarTiposTests {

        @Test
        @DisplayName("Debería retornar todos los tipos disponibles")
        void listarTipos_retornaLista() {
            // Arrange
            List<TipoProducto> tipos = List.of(
                    TipoProducto.builder().idTipo(1L).nombreTipo("Planta").build(),
                    TipoProducto.builder().idTipo(2L).nombreTipo("Semilla").build(),
                    TipoProducto.builder().idTipo(3L).nombreTipo("Accesorio").build(),
                    TipoProducto.builder().idTipo(4L).nombreTipo("Sustrato").build()
            );
            when(productoRepository.listarTipos()).thenReturn(tipos);

            // Act
            List<TipoProducto> resultado = sut.listarTipos();

            // Assert
            assertThat(resultado).hasSize(4);
            assertThat(resultado).extracting(TipoProducto::getNombreTipo)
                    .containsExactlyInAnyOrder("Planta", "Semilla", "Accesorio", "Sustrato");
        }

        @Test
        @DisplayName("Debería delegar al repositorio sin lógica adicional")
        void listarTipos_delegaAlRepositorio() {
            // Arrange
            when(productoRepository.listarTipos()).thenReturn(List.of());

            // Act
            sut.listarTipos();

            // Assert
            verify(productoRepository, times(1)).listarTipos();
        }
    }
}
