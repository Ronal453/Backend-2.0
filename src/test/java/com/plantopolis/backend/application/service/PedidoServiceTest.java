package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.*;
import com.plantopolis.backend.domain.port.out.*;
import com.plantopolis.backend.infrastructure.persistence.entity.*;
import com.plantopolis.backend.infrastructure.persistence.mapper.PedidoMapper;
import com.plantopolis.backend.infrastructure.persistence.repository.*;
import jakarta.persistence.EntityManager;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para PedidoService.
 *
 * Cubre:
 *   - procesarPedido(): happy path, carrito vacío, stock insuficiente,
 *     producto inactivo, método de pago inválido
 *   - obtenerHistorial(): con y sin pedidos
 *   - obtenerDetalle(): pedido propio, pedido ajeno
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PedidoService")
class PedidoServiceTest {

    @Mock private UsuarioRepositoryPort   usuarioRepository;
    @Mock private CarritoRepositoryPort   carritoRepository;
    @Mock private ProductoRepositoryPort  productoRepository;
    @Mock private PedidoRepositoryPort    pedidoRepository;
    @Mock private NotificacionPort        notificacionPort;
    @Mock private PedidoJpaRepository     pedidoJpaRepo;
    @Mock private DetallePedidoJpaRepository detallePedidoRepo;
    @Mock private PagoJpaRepository       pagoJpaRepo;
    @Mock private MetodoPagoJpaRepository metodoPagoRepo;
    @Mock private PedidoMapper            pedidoMapper;
    @Mock private EntityManager           entityManager;

    @InjectMocks
    private PedidoService sut;

    private static final String EMAIL = "ronal@test.com";

    // ── Fixtures ─────────────────────────────────────────────────────────────

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

    private MetodoPagoEntity metodoPago(Long id, String nombre) {
        return new MetodoPagoEntity(id, nombre);
    }

    private PedidoEntity pedidoEntity(Long id) {
        return PedidoEntity.builder()
                .idPedido(id).idUsuario(10L).idEstado(1L)
                .numeroPedido("PL-20240101-0001")
                .fechaPedido(LocalDateTime.now())
                .detalles(new ArrayList<>())
                .build();
    }

    private Pedido pedidoDominio(Long id) {
        return Pedido.builder()
                .idPedido(id).idUsuario(10L)
                .numeroPedido("PL-20240101-0001")
                .estadoDescripcion("PENDIENTE")
                .emailCliente(EMAIL)
                .nombreCliente("Ronal Alarcón")
                .total(new BigDecimal("50000"))
                .detalles(List.of())
                .build();
    }

    @BeforeEach
    void setUpUsuario() {
        lenient().when(usuarioRepository.buscarPorEmail(EMAIL))
                .thenReturn(Optional.of(usuario()));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PROCESAR PEDIDO (CHECKOUT)
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("procesarPedido()")
    class ProcesarPedidoTests {

        private void setupCheckoutExitoso(int cantidadItems) {
            when(carritoRepository.buscarCarritoActivo(10L))
                    .thenReturn(Optional.of(carritoConItems(cantidadItems)));
            when(metodoPagoRepo.findById(1L))
                    .thenReturn(Optional.of(metodoPago(1L, "TARJETA_CREDITO")));
            when(productoRepository.buscarPorId(1L))
                    .thenReturn(Optional.of(productoActivo(20)));
            when(pedidoRepository.generarNumeroPedido())
                    .thenReturn("PL-20240101-0001");
            when(pedidoJpaRepo.saveAndFlush(any())).thenReturn(pedidoEntity(1L));
            when(detallePedidoRepo.saveAll(any())).thenReturn(List.of());
            when(pagoJpaRepo.save(any())).thenReturn(new PagoEntity());
            when(productoRepository.guardar(any())).thenAnswer(i -> i.getArgument(0));
            when(pedidoRepository.buscarPorId(1L))
                    .thenReturn(Optional.of(pedidoDominio(1L)));
            when(pagoJpaRepo.findByIdPedido(1L)).thenReturn(Optional.empty());
        }

        @Test
        @DisplayName("Debería crear el pedido correctamente con datos válidos")
        void procesarPedido_datosValidos_creaElPedido() {
            // Arrange
            setupCheckoutExitoso(2);

            // Act
            Pedido resultado = sut.procesarPedido(EMAIL, 1L, "Calle 123 Bogotá");

            // Assert
            assertThat(resultado).isNotNull();
            assertThat(resultado.getIdPedido()).isEqualTo(1L);
            assertThat(resultado.getNumeroPedido()).isEqualTo("PL-20240101-0001");
        }

        @Test
        @DisplayName("Debería guardar el pedido en BD con los datos correctos")
        void procesarPedido_guardaPedidoConDireccionYEstado() {
            // Arrange
            setupCheckoutExitoso(2);

            // Act
            sut.procesarPedido(EMAIL, 1L, "Calle 456 Medellín");

            // Assert
            ArgumentCaptor<PedidoEntity> captor = ArgumentCaptor.forClass(PedidoEntity.class);
            verify(pedidoJpaRepo).saveAndFlush(captor.capture());
            assertThat(captor.getValue().getDireccionEnvio()).isEqualTo("Calle 456 Medellín");
            assertThat(captor.getValue().getIdEstado()).isEqualTo(1L); // PENDIENTE
        }

        @Test
        @DisplayName("Debería reducir el stock del producto al hacer checkout")
        void procesarPedido_reduceStockDelProducto() {
            // Arrange
            setupCheckoutExitoso(3); // pedir 3 unidades

            // Act
            sut.procesarPedido(EMAIL, 1L, "Calle 123");

            // Assert — stock 20 - 3 = 17
            ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
            verify(productoRepository).guardar(captor.capture());
            assertThat(captor.getValue().getStock()).isEqualTo(17);
        }

       @Test
@DisplayName("Debería marcar el carrito como PROCESADO (estado 3)")
void procesarPedido_marcaCarritoProcesado() {
    // Arrange
    setupCheckoutExitoso(2);

    // Act
    sut.procesarPedido(EMAIL, 1L, "Calle 123");

    // Assert
    // CAMBIO SCHEMA V2: el ID del estado "post-checkout" pasó de 2
    // (CONVERTIDO) a 3 (PROCESADO), por el nuevo orden de inserción
    // en la tabla ESTADOCARRITO.
    verify(carritoRepository).cambiarEstado(100L, 3L);
}
        @Test
        @DisplayName("Debería registrar el pago con el método seleccionado")
        void procesarPedido_registraPagoConMetodo() {
            // Arrange
            setupCheckoutExitoso(2);

            // Act
            sut.procesarPedido(EMAIL, 1L, "Calle 123");

            // Assert
            ArgumentCaptor<PagoEntity> captor = ArgumentCaptor.forClass(PagoEntity.class);
            verify(pagoJpaRepo).save(captor.capture());
            assertThat(captor.getValue().getIdMetodo()).isEqualTo(1L);
            assertThat(captor.getValue().getIdEstadoPago()).isEqualTo(2L); // APROBADO
        }

        @Test
        @DisplayName("Debería enviar email de confirmación al cliente")
        void procesarPedido_enviaEmailDeConfirmacion() {
            // Arrange
            setupCheckoutExitoso(2);

            // Act
            sut.procesarPedido(EMAIL, 1L, "Calle 123");

            // Assert
            verify(notificacionPort).enviarConfirmacionPedido(any(Pedido.class));
        }

        @Test
        @DisplayName("Debería continuar aunque el email falle")
        void procesarPedido_emailFalla_noPropagaExcepcion() {
            // Arrange
            setupCheckoutExitoso(2);
            doThrow(new RuntimeException("SMTP error"))
                    .when(notificacionPort).enviarConfirmacionPedido(any());

            // Act & Assert
            assertThatCode(() -> sut.procesarPedido(EMAIL, 1L, "Calle 123"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debería lanzar excepción cuando el carrito está vacío")
        void procesarPedido_carritoVacio_lanzaExcepcion() {
            // Arrange
            Carrito vacio = Carrito.builder()
                    .idCarrito(100L).idUsuario(10L)
                    .items(List.of()).total(BigDecimal.ZERO).build();
            when(carritoRepository.buscarCarritoActivo(10L))
                    .thenReturn(Optional.of(vacio));

            // Act & Assert
            assertThatThrownBy(() -> sut.procesarPedido(EMAIL, 1L, "Calle 123"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("vacío");

            verify(pedidoJpaRepo, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Debería lanzar excepción cuando no existe carrito activo")
        void procesarPedido_sinCarritoActivo_lanzaExcepcion() {
            // Arrange
            when(carritoRepository.buscarCarritoActivo(10L))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> sut.procesarPedido(EMAIL, 1L, "Calle 123"))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("Debería lanzar excepción cuando el stock es insuficiente")
        void procesarPedido_stockInsuficiente_lanzaExcepcion() {
            // Arrange
            when(carritoRepository.buscarCarritoActivo(10L))
                    .thenReturn(Optional.of(carritoConItems(5))); // pide 5
            when(metodoPagoRepo.findById(1L))
                    .thenReturn(Optional.of(metodoPago(1L, "TARJETA_CREDITO")));
            when(productoRepository.buscarPorId(1L))
                    .thenReturn(Optional.of(productoActivo(2))); // stock = 2

            // Act & Assert
            assertThatThrownBy(() -> sut.procesarPedido(EMAIL, 1L, "Calle 123"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("insuficiente");

            verify(pedidoJpaRepo, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Debería lanzar excepción cuando el producto está inactivo")
        void procesarPedido_productoInactivo_lanzaExcepcion() {
            // Arrange
            Producto inactivo = Producto.builder()
                    .idProducto(1L).stock(10).activo(false).build();
            when(carritoRepository.buscarCarritoActivo(10L))
                    .thenReturn(Optional.of(carritoConItems(2)));
            when(metodoPagoRepo.findById(1L))
                    .thenReturn(Optional.of(metodoPago(1L, "TARJETA_CREDITO")));
            when(productoRepository.buscarPorId(1L))
                    .thenReturn(Optional.of(inactivo));

            // Act & Assert
            assertThatThrownBy(() -> sut.procesarPedido(EMAIL, 1L, "Calle 123"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("no disponible");
        }

        @Test
        @DisplayName("Debería lanzar excepción cuando el método de pago no existe")
        void procesarPedido_metodoPagoInvalido_lanzaExcepcion() {
            // Arrange — el servicio valida el carrito ANTES del método de pago
            when(carritoRepository.buscarCarritoActivo(10L))
                    .thenReturn(Optional.of(carritoConItems(1)));
            when(metodoPagoRepo.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> sut.procesarPedido(EMAIL, 99L, "Calle 123"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Método de pago no válido");
        }

        @Test
        @DisplayName("Debería usar el número de pedido generado")
        void procesarPedido_usaNumeroPedidoGenerado() {
            // Arrange
            setupCheckoutExitoso(2);
            when(pedidoRepository.generarNumeroPedido()).thenReturn("PL-20250525-9999");

            // Act
            sut.procesarPedido(EMAIL, 1L, "Calle 123");

            // Assert
            ArgumentCaptor<PedidoEntity> captor = ArgumentCaptor.forClass(PedidoEntity.class);
            verify(pedidoJpaRepo).saveAndFlush(captor.capture());
            assertThat(captor.getValue().getNumeroPedido()).isEqualTo("PL-20250525-9999");
        }

        @Test
        @DisplayName("Debería guardar los detalles del pedido")
        void procesarPedido_guardaDetalles() {
            // Arrange
            setupCheckoutExitoso(2);

            // Act
            sut.procesarPedido(EMAIL, 1L, "Calle 123");

            // Assert — saveAll debe haberse llamado con los detalles
            verify(detallePedidoRepo).saveAll(anyList());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // OBTENER HISTORIAL
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("obtenerHistorial()")
    class ObtenerHistorialTests {

        @Test
        @DisplayName("Debería retornar la lista de pedidos del usuario")
        void obtenerHistorial_conPedidos_retornaLista() {
            // Arrange
            List<Pedido> pedidos = List.of(
                    pedidoDominio(1L),
                    pedidoDominio(2L)
            );
            when(pedidoRepository.buscarPorUsuario(10L)).thenReturn(pedidos);
            when(pagoJpaRepo.findByIdPedido(anyLong())).thenReturn(Optional.empty());

            // Act
            List<Pedido> resultado = sut.obtenerHistorial(EMAIL);

            // Assert
            assertThat(resultado).hasSize(2);
        }

        @Test
        @DisplayName("Debería retornar lista vacía cuando el usuario no tiene pedidos")
        void obtenerHistorial_sinPedidos_retornaListaVacia() {
            // Arrange
            when(pedidoRepository.buscarPorUsuario(10L)).thenReturn(List.of());

            // Act
            List<Pedido> resultado = sut.obtenerHistorial(EMAIL);

            // Assert
            assertThat(resultado).isEmpty();
        }

        @Test
        @DisplayName("Debería enriquecer el pago de cada pedido del historial")
        void obtenerHistorial_enriquecePagoDeCadaPedido() {
            // Arrange
            Pedido p1 = pedidoDominio(1L);
            Pedido p2 = pedidoDominio(2L);
            when(pedidoRepository.buscarPorUsuario(10L)).thenReturn(List.of(p1, p2));

            PagoEntity pago = PagoEntity.builder()
                    .idPago(1L).idPedido(1L).idMetodo(1L)
                    .monto(new BigDecimal("50000")).build();
            when(pagoJpaRepo.findByIdPedido(anyLong())).thenReturn(Optional.of(pago));
            when(metodoPagoRepo.findById(1L))
                    .thenReturn(Optional.of(metodoPago(1L, "TARJETA_CREDITO")));

            // Act
            sut.obtenerHistorial(EMAIL);

            // Assert — se consultó el pago para cada uno
            verify(pagoJpaRepo, times(2)).findByIdPedido(anyLong());
        }

        @Test
        @DisplayName("Debería lanzar excepción cuando el usuario no existe")
        void obtenerHistorial_usuarioInexistente_lanzaExcepcion() {
            // Arrange
            when(usuarioRepository.buscarPorEmail("noexiste@test.com"))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> sut.obtenerHistorial("noexiste@test.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Usuario no encontrado");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // OBTENER DETALLE
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("obtenerDetalle()")
    class ObtenerDetalleTests {

        @Test
        @DisplayName("Debería retornar el pedido cuando pertenece al usuario")
        void obtenerDetalle_pedidoPropio_loRetorna() {
            // Arrange
            Pedido pedido = pedidoDominio(1L); // idUsuario = 10L
            when(pedidoRepository.buscarPorId(1L)).thenReturn(Optional.of(pedido));
            when(pagoJpaRepo.findByIdPedido(1L)).thenReturn(Optional.empty());

            // Act
            Pedido resultado = sut.obtenerDetalle(EMAIL, 1L);

            // Assert
            assertThat(resultado).isNotNull();
            assertThat(resultado.getIdPedido()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Debería lanzar excepción cuando el pedido pertenece a otro usuario")
        void obtenerDetalle_pedidoAjeno_lanzaExcepcion() {
            // Arrange — pedido de usuario 99, no del 10
            Pedido pedidoAjeno = Pedido.builder()
                    .idPedido(1L).idUsuario(99L)
                    .numeroPedido("PL-AJENO").build();
            when(pedidoRepository.buscarPorId(1L)).thenReturn(Optional.of(pedidoAjeno));

            // Act & Assert
            assertThatThrownBy(() -> sut.obtenerDetalle(EMAIL, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("permisos");
        }

        @Test
        @DisplayName("Debería lanzar excepción cuando el pedido no existe")
        void obtenerDetalle_pedidoInexistente_lanzaExcepcion() {
            // Arrange
            when(pedidoRepository.buscarPorId(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> sut.obtenerDetalle(EMAIL, 99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Pedido no encontrado");
        }

        @Test
        @DisplayName("Debería enriquecer el pago al obtener el detalle")
        void obtenerDetalle_enriquecePago() {
            // Arrange
            Pedido pedido = pedidoDominio(1L);
            when(pedidoRepository.buscarPorId(1L)).thenReturn(Optional.of(pedido));
            PagoEntity pago = PagoEntity.builder()
                    .idPago(1L).idPedido(1L).idMetodo(2L)
                    .monto(new BigDecimal("75000")).build();
            when(pagoJpaRepo.findByIdPedido(1L)).thenReturn(Optional.of(pago));
            when(metodoPagoRepo.findById(2L))
                    .thenReturn(Optional.of(metodoPago(2L, "TARJETA_DEBITO")));

            // Act
            sut.obtenerDetalle(EMAIL, 1L);

            // Assert — se consultó el pago
            verify(pagoJpaRepo).findByIdPedido(1L);
            verify(metodoPagoRepo).findById(2L);
        }
    }
}
