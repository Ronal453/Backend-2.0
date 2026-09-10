package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.Carrito;
import com.plantopolis.backend.domain.model.ItemCarrito;
import com.plantopolis.backend.domain.model.Producto;
import com.plantopolis.backend.domain.model.Usuario;
import com.plantopolis.backend.domain.port.out.CarritoRepositoryPort;
import com.plantopolis.backend.domain.port.out.ProductoRepositoryPort;
import com.plantopolis.backend.domain.port.out.UsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doNothing;

/**
 * Tests unitarios para CarritoService.
 *
 * Cubre:
 *   - verCarrito(): carrito existente, sin carrito (crea uno)
 *   - agregarItem(): producto nuevo, producto existente, sin stock, inactivo
 *   - actualizarCantidad(): actualiza, cantidad cero elimina
 *   - eliminarItem(): delega al repositorio
 *   - vaciarCarrito(): vacía el carrito activo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CarritoService")
class CarritoServiceTest {

    @Mock private CarritoRepositoryPort  carritoRepository;
    @Mock private ProductoRepositoryPort productoRepository;
    @Mock private UsuarioRepositoryPort  usuarioRepository;

    @InjectMocks
    private CarritoService sut;

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private static final String EMAIL = "test@plantopolis.com";

    private Usuario usuario() {
        return Usuario.builder().idUsuario(10L).correo(EMAIL).build();
    }

    private Producto productoActivo(int stock) {
        return Producto.builder()
                .idProducto(1L)
                .nombreProducto("Pothos Dorado")
                .precio(new BigDecimal("25000"))
                .stock(stock)
                .activo(true)
                .build();
    }

    private Carrito carritoVacio() {
        return Carrito.builder()
                .idCarrito(100L)
                .idUsuario(10L)
                .items(new ArrayList<>())
                .total(BigDecimal.ZERO)
                .build();
    }

    private Carrito carritoConItems(int cantidad) {
        ItemCarrito item = ItemCarrito.builder()
                .idItem(1L).idCarrito(100L).idProducto(1L)
                .cantidad(cantidad)
                .precioUnitario(new BigDecimal("25000"))
                .subtotal(new BigDecimal("25000").multiply(BigDecimal.valueOf(cantidad)))
                .build();
        return Carrito.builder()
                .idCarrito(100L).idUsuario(10L)
                .items(List.of(item))
                .total(item.getSubtotal())
                .build();
    }

    @BeforeEach
    void setUpUsuario() {
        lenient().when(usuarioRepository.buscarPorEmail(EMAIL))
                .thenReturn(Optional.of(usuario()));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // VER CARRITO
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("verCarrito()")
    class VerCarritoTests {

        @Test
        @DisplayName("Debería retornar el carrito activo cuando existe")
        void verCarrito_existeCarritoActivo_loRetorna() {
            // Arrange
            Carrito existente = carritoVacio();
            when(carritoRepository.buscarCarritoActivo(10L))
                    .thenReturn(Optional.of(existente));

            // Act
            Carrito resultado = sut.verCarrito(EMAIL);

            // Assert
            assertThat(resultado).isNotNull();
            assertThat(resultado.getIdCarrito()).isEqualTo(100L);
            verify(carritoRepository, never()).crearCarrito(anyLong());
        }

        @Test
        @DisplayName("Debería crear un carrito nuevo cuando no existe ninguno activo")
        void verCarrito_sinCarritoActivo_creaUnoNuevo() {
            // Arrange
            when(carritoRepository.buscarCarritoActivo(10L))
                    .thenReturn(Optional.empty());
            when(carritoRepository.crearCarrito(10L))
                    .thenReturn(carritoVacio());

            // Act
            Carrito resultado = sut.verCarrito(EMAIL);

            // Assert
            assertThat(resultado).isNotNull();
            verify(carritoRepository).crearCarrito(10L);
        }

        @Test
        @DisplayName("Debería lanzar excepción cuando el usuario no existe")
        void verCarrito_usuarioInexistente_lanzaExcepcion() {
            // Arrange
            when(usuarioRepository.buscarPorEmail("noexiste@test.com"))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> sut.verCarrito("noexiste@test.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Usuario no encontrado");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // AGREGAR ITEM
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("agregarItem()")
    class AgregarItemTests {

        @Test
        @DisplayName("Debería agregar un ítem nuevo cuando el producto no está en el carrito")
        void agregarItem_productoNuevo_creaItem() {
            // Arrange
            when(productoRepository.buscarPorId(1L))
                    .thenReturn(Optional.of(productoActivo(10)));
            when(carritoRepository.buscarCarritoActivo(10L))
                    .thenReturn(Optional.of(carritoVacio()));
            when(carritoRepository.buscarItem(100L, 1L))
                    .thenReturn(Optional.empty());
            when(carritoRepository.guardarItem(any()))
                    .thenReturn(new ItemCarrito());
            when(carritoRepository.buscarCarritoActivo(10L))
                    .thenReturn(Optional.of(carritoConItems(2)));

            // Act
            Carrito resultado = sut.agregarItem(EMAIL, 1L, 2);

            // Assert
            verify(carritoRepository).guardarItem(any(ItemCarrito.class));
            assertThat(resultado).isNotNull();
        }

        @Test
        @DisplayName("Debería sumar cantidad cuando el producto ya está en el carrito")
        void agregarItem_productoExistente_sumaLaCantidad() {
            // Arrange
            ItemCarrito itemExistente = ItemCarrito.builder()
                    .idItem(1L).idCarrito(100L).idProducto(1L).cantidad(3).build();

            when(productoRepository.buscarPorId(1L))
                    .thenReturn(Optional.of(productoActivo(10)));
            when(carritoRepository.buscarCarritoActivo(10L))
                    .thenReturn(Optional.of(carritoVacio()));
            when(carritoRepository.buscarItem(100L, 1L))
                    .thenReturn(Optional.of(itemExistente));
            when(carritoRepository.guardarItem(any())).thenReturn(itemExistente);
            // Segunda llamada para retornar el carrito actualizado
            when(carritoRepository.buscarCarritoActivo(10L))
                    .thenReturn(Optional.of(carritoConItems(5)));

            // Act
            sut.agregarItem(EMAIL, 1L, 2);

            // Assert — verifica que se guarda con la cantidad sumada (3+2=5)
            ArgumentCaptor<ItemCarrito> captor = ArgumentCaptor.forClass(ItemCarrito.class);
            verify(carritoRepository).guardarItem(captor.capture());
            assertThat(captor.getValue().getCantidad()).isEqualTo(5);
        }

        @Test
        @DisplayName("Debería lanzar excepción cuando no hay suficiente stock")
        void agregarItem_stockInsuficiente_lanzaExcepcion() {
            // Arrange
            when(productoRepository.buscarPorId(1L))
                    .thenReturn(Optional.of(productoActivo(1))); // stock = 1

            // Act & Assert — pedir 5 con stock = 1
            assertThatThrownBy(() -> sut.agregarItem(EMAIL, 1L, 5))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Stock insuficiente");

            verify(carritoRepository, never()).guardarItem(any());
        }

        @Test
        @DisplayName("Debería lanzar excepción cuando el producto está inactivo")
        void agregarItem_productoInactivo_lanzaExcepcion() {
            // Arrange
            Producto inactivo = Producto.builder()
                    .idProducto(1L).stock(10).activo(false).build();
            when(productoRepository.buscarPorId(1L)).thenReturn(Optional.of(inactivo));

            // Act & Assert
            assertThatThrownBy(() -> sut.agregarItem(EMAIL, 1L, 1))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("no disponible");
        }

        @Test
        @DisplayName("Debería lanzar excepción cuando el producto no existe")
        void agregarItem_productoNoExiste_lanzaExcepcion() {
            // Arrange
            when(productoRepository.buscarPorId(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> sut.agregarItem(EMAIL, 99L, 1))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Producto no encontrado");
        }

        @Test
        @DisplayName("Debería lanzar excepción cuando stock acumulado supera el disponible")
        void agregarItem_stockAcumuladoInsuficiente_lanzaExcepcion() {
            // Arrange — stock=5, ya hay 4 en carrito, pide 2 más (total=6 > 5)
            ItemCarrito itemExistente = ItemCarrito.builder()
                    .idItem(1L).idCarrito(100L).idProducto(1L).cantidad(4).build();

            when(productoRepository.buscarPorId(1L))
                    .thenReturn(Optional.of(productoActivo(5)));
            when(carritoRepository.buscarCarritoActivo(10L))
                    .thenReturn(Optional.of(carritoVacio()));
            when(carritoRepository.buscarItem(100L, 1L))
                    .thenReturn(Optional.of(itemExistente));

            // Act & Assert
            assertThatThrownBy(() -> sut.agregarItem(EMAIL, 1L, 2))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Stock insuficiente");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ACTUALIZAR CANTIDAD
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("actualizarCantidad()")
    class ActualizarCantidadTests {

        private ItemCarrito itemExistente() {
            return ItemCarrito.builder()
                    .idItem(1L).idCarrito(100L).idProducto(1L).cantidad(3).build();
        }

        @Test
        @DisplayName("Debería actualizar la cantidad cuando hay stock suficiente")
        void actualizarCantidad_stockSuficiente_actualiza() {
            // Arrange
            when(carritoRepository.buscarItemPorId(1L))
                    .thenReturn(Optional.of(itemExistente()));
            when(productoRepository.buscarPorId(1L))
                    .thenReturn(Optional.of(productoActivo(10)));
            when(carritoRepository.guardarItem(any())).thenReturn(itemExistente());
            when(carritoRepository.buscarCarritoActivo(10L))
                    .thenReturn(Optional.of(carritoConItems(5)));

            // Act
            Carrito resultado = sut.actualizarCantidad(EMAIL, 1L, 5);

            // Assert
            assertThat(resultado).isNotNull();
            ArgumentCaptor<ItemCarrito> captor = ArgumentCaptor.forClass(ItemCarrito.class);
            verify(carritoRepository).guardarItem(captor.capture());
            assertThat(captor.getValue().getCantidad()).isEqualTo(5);
        }

        @Test
        @DisplayName("Debería lanzar excepción cuando stock insuficiente para nueva cantidad")
        void actualizarCantidad_stockInsuficiente_lanzaExcepcion() {
            // Arrange
            when(carritoRepository.buscarItemPorId(1L))
                    .thenReturn(Optional.of(itemExistente()));
            when(productoRepository.buscarPorId(1L))
                    .thenReturn(Optional.of(productoActivo(2))); // stock = 2

            // Act & Assert — pedir 10 con stock 2
            assertThatThrownBy(() -> sut.actualizarCantidad(EMAIL, 1L, 10))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Stock insuficiente");
        }

        @Test
        @DisplayName("Debería eliminar el ítem cuando la cantidad es 0")
        void actualizarCantidad_cantidadCero_eliminaItem() {
            // Arrange
            // eliminarItem() retorna void — Mockito lo stubea como no-op por defecto
            doNothing().when(carritoRepository).eliminarItem(1L);
            when(carritoRepository.buscarCarritoActivo(10L))
                    .thenReturn(Optional.of(carritoVacio()));

            // Act
            Carrito resultado = sut.actualizarCantidad(EMAIL, 1L, 0);

            // Assert
            verify(carritoRepository).eliminarItem(1L);
            verify(carritoRepository, never()).guardarItem(any());
            assertThat(resultado).isNotNull();
        }

        @Test
        @DisplayName("Debería eliminar el ítem cuando la cantidad es negativa")
        void actualizarCantidad_cantidadNegativa_eliminaItem() {
            // Arrange
            // eliminarItem() retorna void — no-op por defecto en Mockito
            doNothing().when(carritoRepository).eliminarItem(1L);
            when(carritoRepository.buscarCarritoActivo(10L))
                    .thenReturn(Optional.of(carritoVacio()));

            // Act
            sut.actualizarCantidad(EMAIL, 1L, -3);

            // Assert
            verify(carritoRepository).eliminarItem(1L);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ELIMINAR ITEM
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("eliminarItem()")
    class EliminarItemTests {

        @Test
        @DisplayName("Debería delegar la eliminación al repositorio")
        void eliminarItem_delegaAlRepositorio() {
            // Arrange
            when(carritoRepository.buscarCarritoActivo(10L))
                    .thenReturn(Optional.of(carritoVacio()));

            // Act
            sut.eliminarItem(EMAIL, 1L);

            // Assert
            verify(carritoRepository).eliminarItem(1L);
        }

        @Test
        @DisplayName("Debería retornar el carrito actualizado tras eliminar")
        void eliminarItem_retornaCarritoActualizado() {
            // Arrange
            when(carritoRepository.buscarCarritoActivo(10L))
                    .thenReturn(Optional.of(carritoVacio()));

            // Act
            Carrito resultado = sut.eliminarItem(EMAIL, 1L);

            // Assert
            assertThat(resultado).isNotNull();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // VACIAR CARRITO
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("vaciarCarrito()")
    class VaciarCarritoTests {

        @Test
        @DisplayName("Debería vaciar el carrito activo cuando existe")
        void vaciarCarrito_existeCarrito_loVacia() {
            // Arrange
            when(carritoRepository.buscarCarritoActivo(10L))
                    .thenReturn(Optional.of(carritoVacio()));

            // Act
            sut.vaciarCarrito(EMAIL);

            // Assert
            verify(carritoRepository).vaciarCarrito(100L);
        }

        @Test
        @DisplayName("No debería lanzar excepción cuando no hay carrito activo")
        void vaciarCarrito_sinCarritoActivo_noHaceNada() {
            // Arrange
            when(carritoRepository.buscarCarritoActivo(10L))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatCode(() -> sut.vaciarCarrito(EMAIL))
                    .doesNotThrowAnyException();

            verify(carritoRepository, never()).vaciarCarrito(anyLong());
        }
    }
}
