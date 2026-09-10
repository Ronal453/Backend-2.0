package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.Producto;
import com.plantopolis.backend.domain.port.out.ProductoRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
 * Tests unitarios para AdminProductoService.
 *
 * Cubre:
 *   - listarTodos(): activos + inactivos con filtros
 *   - crear(): nuevo producto, activo por defecto, ID null
 *   - actualizar(): campos parciales, producto no encontrado
 *   - activar(): cambia activo=true
 *   - desactivar(): cambia activo=false
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminProductoService")
class AdminProductoServiceTest {

    @Mock private ProductoRepositoryPort productoRepository;

    @InjectMocks
    private AdminProductoService sut;

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private Producto productoBase(boolean activo) {
        return Producto.builder()
                .idProducto(1L)
                .nombreProducto("Pothos Dorado")
                .descripcion("Planta tropical")
                .precio(new BigDecimal("25000"))
                .stock(20)
                .idCategoria(1L)
                .idTipo(1L)
                .activo(activo)
                .build();
    }

    private Pageable pageable() {
        return PageRequest.of(0, 20);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LISTAR TODOS (ADMIN)
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("listarTodos()")
    class ListarTodosTests {

        @Test
        @DisplayName("Debería retornar activos e inactivos")
        void listarTodos_retornaActivosEInactivos() {
            // Arrange
            Producto inactivo = Producto.builder()
                    .idProducto(2L).nombreProducto("Pothos Dorado")
                    .precio(new BigDecimal("25000")).stock(20)
                    .idCategoria(1L).idTipo(1L).activo(false).build();
            List<Producto> todos = List.of(productoBase(true), inactivo);
            when(productoRepository.buscarTodosAdmin(any(), any(), any(), any()))
                    .thenReturn(new PageImpl<>(todos, pageable(), 2));

            // Act
            Page<Producto> resultado = sut.listarTodos(null, null, null, pageable());

            // Assert
            assertThat(resultado.getContent()).hasSize(2);
            assertThat(resultado.getContent())
                    .extracting(Producto::getActivo)
                    .containsExactly(true, false);
        }

        @Test
        @DisplayName("Debería filtrar por nombre cuando se proporciona")
        void listarTodos_conFiltroNombre_delegaAlRepositorio() {
            // Arrange
            when(productoRepository.buscarTodosAdmin(eq("Ficus"), any(), any(), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            // Act
            sut.listarTodos("Ficus", null, null, pageable());

            // Assert
            verify(productoRepository).buscarTodosAdmin("Ficus", null, null, pageable());
        }

        @Test
        @DisplayName("Debería delegar a buscarTodosAdmin (no al catálogo público)")
        void listarTodos_usaMetodoAdmin_noPublico() {
            // Arrange
            when(productoRepository.buscarTodosAdmin(any(), any(), any(), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            // Act
            sut.listarTodos(null, null, null, pageable());

            // Assert
            verify(productoRepository).buscarTodosAdmin(any(), any(), any(), any());
            verify(productoRepository, never()).buscarConFiltros(
                    any(), any(), any(), any(), any(), any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CREAR
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("crear()")
    class CrearTests {

        @Test
        @DisplayName("Debería crear el producto con activo=true por defecto")
        void crear_debePonerActivoTrue() {
            // Arrange
            Producto nuevo = productoBase(false); // viene sin activo
            nuevo.setIdProducto(null);
            when(productoRepository.guardar(any())).thenReturn(productoBase(true));

            // Act
            sut.crear(nuevo);

            // Assert
            ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
            verify(productoRepository).guardar(captor.capture());
            assertThat(captor.getValue().getActivo()).isTrue();
        }

        @Test
        @DisplayName("Debería limpiar el ID antes de guardar (es un POST)")
        void crear_debeLimpiarId() {
            // Arrange
            Producto conId = productoBase(true); // idProducto = 1L
            when(productoRepository.guardar(any())).thenReturn(productoBase(true));

            // Act
            sut.crear(conId);

            // Assert
            ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
            verify(productoRepository).guardar(captor.capture());
            assertThat(captor.getValue().getIdProducto()).isNull();
        }

        @Test
        @DisplayName("Debería retornar el producto guardado con su ID asignado")
        void crear_retornaProductoConId() {
            // Arrange
            Producto guardado = productoBase(true);
            guardado.setIdProducto(99L);
            when(productoRepository.guardar(any())).thenReturn(guardado);

            // Act
            Producto resultado = sut.crear(productoBase(true));

            // Assert
            assertThat(resultado.getIdProducto()).isEqualTo(99L);
        }

        @Test
        @DisplayName("Debería llamar guardar exactamente una vez")
        void crear_llamaGuardarUnaVez() {
            // Arrange
            when(productoRepository.guardar(any())).thenReturn(productoBase(true));

            // Act
            sut.crear(productoBase(true));

            // Assert
            verify(productoRepository, times(1)).guardar(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ACTUALIZAR
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("actualizar()")
    class ActualizarTests {

        @Test
        @DisplayName("Debería actualizar nombre y precio cuando se envían")
        void actualizar_nombreYPrecio_losActualiza() {
            // Arrange
            Producto existente = productoBase(true);
            when(productoRepository.buscarPorId(1L)).thenReturn(Optional.of(existente));
            when(productoRepository.guardar(any())).thenAnswer(i -> i.getArgument(0));

            Producto datos = Producto.builder()
                    .nombreProducto("Pothos Silver")
                    .precio(new BigDecimal("30000"))
                    .build();

            // Act
            Producto resultado = sut.actualizar(1L, datos);

            // Assert
            assertThat(resultado.getNombreProducto()).isEqualTo("Pothos Silver");
            assertThat(resultado.getPrecio()).isEqualByComparingTo("30000");
        }

        @Test
        @DisplayName("Debería actualizar solo el stock sin afectar otros campos")
        void actualizar_soloStock_noAfectaOtrosCampos() {
            // Arrange
            Producto existente = productoBase(true);
            existente.setPrecio(new BigDecimal("25000"));
            when(productoRepository.buscarPorId(1L)).thenReturn(Optional.of(existente));
            when(productoRepository.guardar(any())).thenAnswer(i -> i.getArgument(0));

            Producto datos = Producto.builder().stock(100).build();

            // Act
            Producto resultado = sut.actualizar(1L, datos);

            // Assert
            assertThat(resultado.getStock()).isEqualTo(100);
            assertThat(resultado.getPrecio()).isEqualByComparingTo("25000"); // sin cambio
            assertThat(resultado.getNombreProducto()).isEqualTo("Pothos Dorado"); // sin cambio
        }

        @Test
        @DisplayName("Debería lanzar excepción cuando el producto no existe")
        void actualizar_productoInexistente_lanzaExcepcion() {
            // Arrange
            when(productoRepository.buscarPorId(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> sut.actualizar(99L, productoBase(true)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Producto no encontrado")
                    .hasMessageContaining("99");

            verify(productoRepository, never()).guardar(any());
        }

        @Test
        @DisplayName("Debería preservar el estado activo del producto al actualizar")
        void actualizar_preservaEstadoActivo() {
            // Arrange
            Producto inactivo = productoBase(false);
            when(productoRepository.buscarPorId(1L)).thenReturn(Optional.of(inactivo));
            when(productoRepository.guardar(any())).thenAnswer(i -> i.getArgument(0));

            Producto datos = Producto.builder().stock(50).build();

            // Act
            Producto resultado = sut.actualizar(1L, datos);

            // Assert — el activo no cambia al actualizar stock
            assertThat(resultado.getActivo()).isFalse();
        }

        @Test
        @DisplayName("Debería actualizar idCategoria e idTipo cuando se envían")
        void actualizar_categoriaYTipo_losActualiza() {
            // Arrange
            Producto existente = productoBase(true);
            when(productoRepository.buscarPorId(1L)).thenReturn(Optional.of(existente));
            when(productoRepository.guardar(any())).thenAnswer(i -> i.getArgument(0));

            Producto datos = Producto.builder()
                    .idCategoria(3L).idTipo(2L).build();

            // Act
            Producto resultado = sut.actualizar(1L, datos);

            // Assert
            assertThat(resultado.getIdCategoria()).isEqualTo(3L);
            assertThat(resultado.getIdTipo()).isEqualTo(2L);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ACTIVAR
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("activar()")
    class ActivarTests {

        @Test
        @DisplayName("Debería poner activo=true en el producto")
        void activar_ponerActivoTrue() {
            // Arrange
            Producto inactivo = productoBase(false);
            when(productoRepository.buscarPorId(1L)).thenReturn(Optional.of(inactivo));
            when(productoRepository.guardar(any())).thenAnswer(i -> i.getArgument(0));

            // Act
            Producto resultado = sut.activar(1L);

            // Assert
            assertThat(resultado.getActivo()).isTrue();
        }

        @Test
        @DisplayName("Debería guardar el producto con activo=true")
        void activar_guardaConActivoTrue() {
            // Arrange
            when(productoRepository.buscarPorId(1L))
                    .thenReturn(Optional.of(productoBase(false)));
            when(productoRepository.guardar(any())).thenAnswer(i -> i.getArgument(0));

            // Act
            sut.activar(1L);

            // Assert
            ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
            verify(productoRepository).guardar(captor.capture());
            assertThat(captor.getValue().getActivo()).isTrue();
        }

        @Test
        @DisplayName("Debería lanzar excepción cuando el producto no existe")
        void activar_productoInexistente_lanzaExcepcion() {
            // Arrange
            when(productoRepository.buscarPorId(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> sut.activar(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("99");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DESACTIVAR
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("desactivar()")
    class DesactivarTests {

        @Test
        @DisplayName("Debería poner activo=false en el producto")
        void desactivar_ponerActivoFalse() {
            // Arrange
            Producto activo = productoBase(true);
            when(productoRepository.buscarPorId(1L)).thenReturn(Optional.of(activo));
            when(productoRepository.guardar(any())).thenAnswer(i -> i.getArgument(0));

            // Act
            Producto resultado = sut.desactivar(1L);

            // Assert
            assertThat(resultado.getActivo()).isFalse();
        }

        @Test
        @DisplayName("Debería guardar el producto con activo=false")
        void desactivar_guardaConActivoFalse() {
            // Arrange
            when(productoRepository.buscarPorId(1L))
                    .thenReturn(Optional.of(productoBase(true)));
            when(productoRepository.guardar(any())).thenAnswer(i -> i.getArgument(0));

            // Act
            sut.desactivar(1L);

            // Assert
            ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
            verify(productoRepository).guardar(captor.capture());
            assertThat(captor.getValue().getActivo()).isFalse();
        }

        @Test
        @DisplayName("Debería lanzar excepción cuando el producto no existe")
        void desactivar_productoInexistente_lanzaExcepcion() {
            // Arrange
            when(productoRepository.buscarPorId(5L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> sut.desactivar(5L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("5");

            verify(productoRepository, never()).guardar(any());
        }

        @Test
        @DisplayName("Debería preservar todos los demás datos del producto al desactivar")
        void desactivar_preservaOtrosDatos() {
            // Arrange
            Producto original = productoBase(true);
            when(productoRepository.buscarPorId(1L)).thenReturn(Optional.of(original));
            when(productoRepository.guardar(any())).thenAnswer(i -> i.getArgument(0));

            // Act
            Producto resultado = sut.desactivar(1L);

            // Assert
            assertThat(resultado.getNombreProducto()).isEqualTo("Pothos Dorado");
            assertThat(resultado.getPrecio()).isEqualByComparingTo("25000");
            assertThat(resultado.getStock()).isEqualTo(20);
            assertThat(resultado.getActivo()).isFalse(); // solo esto cambia
        }
    }
}
