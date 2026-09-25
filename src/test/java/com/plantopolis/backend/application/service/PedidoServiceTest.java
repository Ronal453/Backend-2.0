package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.*;
import com.plantopolis.backend.domain.port.out.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PedidoService")
class PedidoServiceTest {

    @Mock private UsuarioRepositoryPort   usuarioRepository;
    @Mock private CarritoRepositoryPort   carritoRepository;
    @Mock private ProductoRepositoryPort  productoRepository;
    @Mock private PedidoRepositoryPort    pedidoRepository;
    @Mock private NotificacionPort        notificacionPort;

    @InjectMocks
    private PedidoService sut;

    private static final String EMAIL = "ronal@test.com";

    private Usuario usuario() {
        return Usuario.builder()
                .idUsuario(10L).correo(EMAIL)
                .nombreCompleto("Ronal Alarcón").build();
    }

    private Producto productoActivo(int stock) {
        return Producto.builder()
                .idProducto(1L).nombreProducto("Pothos Dorado")
                .precio(new BigDecimal("25000"))
                .stock(stock).activo(true).build();
    }

    private ItemCarrito item(int cantidad) {
        return ItemCarrito.builder()
                .idItem(1L).idCarrito(100L).idProducto(1L)
                .nombreProducto("Pothos Dorado")
                .precioUnitario(new BigDecimal("25000"))
                .cantidad(cantidad)
                .subtotal(new BigDecimal("25000").multiply(BigDecimal.valueOf(cantidad)))
                .build();
    }

    private Carrito carritoConItems(int cantidad) {
        BigDecimal total = new BigDecimal("25000").multiply(BigDecimal.valueOf(cantidad));
        return Carrito.builder()
                .idCarrito(100L).idUsuario(10L)
                .items(List.of(item(cantidad)))
                .total(total).build();
    }

    private Pedido pedidoDominio(Long id) {
        return Pedido.builder()
                .idPedido(id).idUsuario(10L)
                .numeroPedido("PL-20240101-0001")
                .fechaPedido(LocalDateTime.now())
                .subtotal(new BigDecimal("25000"))
                .impuestos(new BigDecimal("4750"))
                .total(new BigDecimal("29750"))
                .estadoDescripcion("PENDIENTE")
                .build();
    }

    @Nested
    @DisplayName("procesarPedido()")
    class ProcesarPedidoTests {

        @Test
        @DisplayName("Debe fallar si usuario no existe")
        void procesarPedido_usuarioNoExiste_lanzaExcepcion() {
            when(usuarioRepository.buscarPorEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sut.procesarPedido(EMAIL, 1L, "Dir"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Usuario no encontrado");
        }

        @Test
        @DisplayName("Debe fallar si carrito está vacío")
        void procesarPedido_carritoVacio_lanzaExcepcion() {
            when(usuarioRepository.buscarPorEmail(EMAIL)).thenReturn(Optional.of(usuario()));
            when(carritoRepository.buscarCarritoActivo(10L)).thenReturn(Optional.of(
                    Carrito.builder().idCarrito(100L).items(new ArrayList<>()).build()
            ));

            assertThatThrownBy(() -> sut.procesarPedido(EMAIL, 1L, "Dir"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("El carrito está vacío");
        }

        @Test
        @DisplayName("Debe fallar si producto no tiene stock")
        void procesarPedido_stockInsuficiente_lanzaExcepcion() {
            when(usuarioRepository.buscarPorEmail(EMAIL)).thenReturn(Optional.of(usuario()));
            when(carritoRepository.buscarCarritoActivo(10L)).thenReturn(Optional.of(carritoConItems(5)));
            when(productoRepository.buscarPorId(1L)).thenReturn(Optional.of(productoActivo(2)));

            assertThatThrownBy(() -> sut.procesarPedido(EMAIL, 1L, "Dir"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Stock insuficiente");
        }

        @Test
        @DisplayName("Crea el pedido correctamente y limpia dependencias de JPA")
        void procesarPedido_exito() {
            when(usuarioRepository.buscarPorEmail(EMAIL)).thenReturn(Optional.of(usuario()));
            when(carritoRepository.buscarCarritoActivo(10L)).thenReturn(Optional.of(carritoConItems(2)));
            when(productoRepository.buscarPorId(1L)).thenReturn(Optional.of(productoActivo(10)));
            when(pedidoRepository.generarNumeroPedido()).thenReturn("PL-TEST-01");
            when(pedidoRepository.procesarNuevoPedido(any(), anyLong())).thenReturn(pedidoDominio(50L));

            Pedido resultado = sut.procesarPedido(EMAIL, 1L, "Dir 123");

            assertThat(resultado.getIdPedido()).isEqualTo(50L);
            verify(pedidoRepository).procesarNuevoPedido(any(Pedido.class), eq(1L));
            verify(carritoRepository).cambiarEstado(100L, 3L);
            verify(notificacionPort).enviarConfirmacionPedido(any(Pedido.class));

            ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
            verify(productoRepository).guardar(captor.capture());
            assertThat(captor.getValue().getStock()).isEqualTo(8);
        }
    }

    @Nested
    @DisplayName("obtenerHistorial()")
    class ObtenerHistorialTests {

        @Test
        @DisplayName("Retorna pedidos del usuario")
        void obtenerHistorial_exito() {
            when(usuarioRepository.buscarPorEmail(EMAIL)).thenReturn(Optional.of(usuario()));
            when(pedidoRepository.buscarPorUsuario(10L)).thenReturn(List.of(pedidoDominio(1L)));

            List<Pedido> res = sut.obtenerHistorial(EMAIL);
            assertThat(res).hasSize(1);
        }
    }

    @Nested
    @DisplayName("obtenerDetalle()")
    class ObtenerDetalleTests {

        @Test
        @DisplayName("Deniega acceso si no es el usuario")
        void obtenerDetalle_noPropietario() {
            when(usuarioRepository.buscarPorEmail(EMAIL)).thenReturn(Optional.of(usuario()));
            Pedido ajeno = pedidoDominio(1L);
            ajeno.setIdUsuario(999L);
            when(pedidoRepository.buscarPorIdEnriquecido(1L)).thenReturn(ajeno);

            assertThatThrownBy(() -> sut.obtenerDetalle(EMAIL, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("No tienes permisos");
        }

        @Test
        @DisplayName("Retorna el pedido enriquecido")
        void obtenerDetalle_exito() {
            when(usuarioRepository.buscarPorEmail(EMAIL)).thenReturn(Optional.of(usuario()));
            when(pedidoRepository.buscarPorIdEnriquecido(1L)).thenReturn(pedidoDominio(1L));

            Pedido res = sut.obtenerDetalle(EMAIL, 1L);
            assertThat(res.getIdPedido()).isEqualTo(1L);
        }
    }
}
