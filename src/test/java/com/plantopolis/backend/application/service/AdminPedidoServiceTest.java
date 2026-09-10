package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.Pedido;
import com.plantopolis.backend.domain.port.out.NotificacionPort;
import com.plantopolis.backend.domain.port.out.ProductoRepositoryPort;
import com.plantopolis.backend.infrastructure.persistence.entity.*;
import com.plantopolis.backend.infrastructure.persistence.mapper.PedidoMapper;
import com.plantopolis.backend.infrastructure.persistence.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para AdminPedidoService.
 * Nota: EntityManager usa @PersistenceContext (no @Autowired), por eso
 * Mockito no lo inyecta automáticamente — se inyecta con ReflectionTestUtils.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminPedidoService")
class AdminPedidoServiceTest {

    @Mock private PedidoJpaRepository       pedidoJpaRepo;
    @Mock private EstadoPedidoJpaRepository estadoPedidoRepo;
    @Mock private ProductoRepositoryPort    productoRepository;
    @Mock private NotificacionPort          notificacionPort;
    @Mock private PedidoMapper              pedidoMapper;
    @Mock private PagoJpaRepository         pagoJpaRepo;
    @Mock private MetodoPagoJpaRepository   metodoPagoRepo;
    @Mock private EntityManager             entityManager;

    @InjectMocks
    private AdminPedidoService sut;

    /**
     * @PersistenceContext no es reconocido por Mockito @InjectMocks.
     * Se inyecta el mock manualmente con ReflectionTestUtils.
     */
    @BeforeEach
    void injectEntityManager() {
        ReflectionTestUtils.setField(sut, "entityManager", entityManager);
    }

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private EstadoPedidoEntity estadoEntity(Long id, String descripcion) {
        return new EstadoPedidoEntity(id, descripcion);
    }

    private PedidoEntity pedidoEntity(Long id, EstadoPedidoEntity estado) {
    return PedidoEntity.builder()
            .idPedido(id).idUsuario(10L)
            // CAMBIO SCHEMA V2: EstadoPedidoEntity ya no expone getIdEstado(),
            // ahora es getIdEstadoPedido() (coincide con la PK renombrada
            // ID_ESTADO_PEDIDO en la tabla ESTADOPEDIDO).
            .idEstado(estado.getIdEstadoPedido())
            .numeroPedido("PL-20240101-1234")
            .fechaPedido(LocalDateTime.now())
            .estado(estado)
            .usuario(usuarioEntity())
            .detalles(List.of())
            .build();
}

    private UsuarioEntity usuarioEntity() {
        return UsuarioEntity.builder()
                .idUsuario(10L).nombreCompleto("Ronal Alarcón")
                .correo("ronal@test.com").idRol(1L).build();
    }

    private Pedido pedidoDominio(Long id, String estado) {
        return Pedido.builder()
                .idPedido(id).idUsuario(10L)
                .numeroPedido("PL-20240101-1234")
                .estadoDescripcion(estado)
                .emailCliente("ronal@test.com")
                .nombreCliente("Ronal Alarcón")
                .total(new BigDecimal("50000"))
                .detalles(List.of())
                .build();
    }

    private void setupCambioEstadoExitoso(Long idPedido, String estadoActual, String nuevoEstado) {
        EstadoPedidoEntity estadoActualEnt = estadoEntity(1L, estadoActual);
        PedidoEntity entity = pedidoEntity(idPedido, estadoActualEnt);

        EstadoPedidoEntity nuevoEstadoEnt = estadoEntity(2L, nuevoEstado);
        PedidoEntity entityActualizado = pedidoEntity(idPedido, nuevoEstadoEnt);
        Pedido dominioActualizado = pedidoDominio(idPedido, nuevoEstado);

        when(pedidoJpaRepo.findByIdWithRelations(idPedido))
                .thenReturn(Optional.of(entity))
                .thenReturn(Optional.of(entityActualizado));
        when(estadoPedidoRepo.findByDescripcionEstado(nuevoEstado))
                .thenReturn(Optional.of(nuevoEstadoEnt));
        when(pedidoJpaRepo.saveAndFlush(any())).thenReturn(entityActualizado);
        when(pedidoMapper.toDomain(entityActualizado)).thenReturn(dominioActualizado);
        when(pagoJpaRepo.findByIdPedido(idPedido)).thenReturn(Optional.empty());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LISTAR TODOS
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("listarTodos()")
    class ListarTodosTests {

        @Test
        @DisplayName("Debería retornar página de pedidos sin filtro de estado")
        void listarTodos_sinFiltro_retornaTodos() {
            EstadoPedidoEntity estado = estadoEntity(1L, "PENDIENTE");
            PedidoEntity entity = pedidoEntity(1L, estado);
            Pedido dominio = pedidoDominio(1L, "PENDIENTE");
            Pageable pag = PageRequest.of(0, 20);

            when(pedidoJpaRepo.findAllAdminWithFilters(isNull(), eq(pag)))
                    .thenReturn(new PageImpl<>(List.of(entity), pag, 1));
            when(pedidoMapper.toDomain(entity)).thenReturn(dominio);
            when(pagoJpaRepo.findByIdPedido(1L)).thenReturn(Optional.empty());

            Page<Pedido> resultado = sut.listarTodos(null, pag);

            assertThat(resultado.getContent()).hasSize(1);
            assertThat(resultado.getContent().get(0).getEstadoDescripcion()).isEqualTo("PENDIENTE");
        }

        @Test
        @DisplayName("Debería filtrar por estado convirtiendo a mayúsculas")
        void listarTodos_conFiltroEstado_pasaFiltroUpperCase() {
            Pageable pag = PageRequest.of(0, 20);
            when(pedidoJpaRepo.findAllAdminWithFilters(eq("ENVIADO"), eq(pag)))
                    .thenReturn(new PageImpl<>(List.of(), pag, 0));

            sut.listarTodos("enviado", pag);

            verify(pedidoJpaRepo).findAllAdminWithFilters("ENVIADO", pag);
        }

        @Test
        @DisplayName("Debería tratar estado en blanco como sin filtro (null)")
        void listarTodos_estadoVacio_usaNull() {
            Pageable pag = PageRequest.of(0, 20);
            when(pedidoJpaRepo.findAllAdminWithFilters(isNull(), eq(pag)))
                    .thenReturn(new PageImpl<>(List.of()));

            sut.listarTodos("   ", pag);

            verify(pedidoJpaRepo).findAllAdminWithFilters(null, pag);
        }

        @Test
        @DisplayName("Debería enriquecer el pago de cada pedido en la lista")
        void listarTodos_enriquecePagoDeCadaPedido() {
            EstadoPedidoEntity estado = estadoEntity(1L, "PENDIENTE");
            PedidoEntity entity = pedidoEntity(1L, estado);
            Pedido dominio = pedidoDominio(1L, "PENDIENTE");
            Pageable pag = PageRequest.of(0, 20);

            PagoEntity pago = PagoEntity.builder()
                    .idPago(1L).idPedido(1L).idMetodo(1L)
                    .monto(new BigDecimal("50000")).build();

            when(pedidoJpaRepo.findAllAdminWithFilters(isNull(), eq(pag)))
                    .thenReturn(new PageImpl<>(List.of(entity)));
            when(pedidoMapper.toDomain(entity)).thenReturn(dominio);
            when(pagoJpaRepo.findByIdPedido(1L)).thenReturn(Optional.of(pago));
            when(metodoPagoRepo.findById(1L))
                    .thenReturn(Optional.of(new MetodoPagoEntity(1L, "TARJETA_CREDITO")));

            sut.listarTodos(null, pag);

            verify(pagoJpaRepo).findByIdPedido(1L);
            verify(metodoPagoRepo).findById(1L);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ACTUALIZAR ESTADO
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("actualizarEstado()")
    class ActualizarEstadoTests {

        @Test
        @DisplayName("Debería cambiar PENDIENTE → PREPARANDO")
        void actualizarEstado_pendienteAPreparando_exitoso() {
            setupCambioEstadoExitoso(1L, "PENDIENTE", "PREPARANDO");
            Pedido resultado = sut.actualizarEstado(1L, "PREPARANDO");
            assertThat(resultado.getEstadoDescripcion()).isEqualTo("PREPARANDO");
            verify(pedidoJpaRepo).saveAndFlush(any());
        }

        @Test
        @DisplayName("Debería cambiar PREPARANDO → ENVIADO")
        void actualizarEstado_preparandoAEnviado_exitoso() {
            setupCambioEstadoExitoso(1L, "PREPARANDO", "ENVIADO");
            Pedido resultado = sut.actualizarEstado(1L, "ENVIADO");
            assertThat(resultado.getEstadoDescripcion()).isEqualTo("ENVIADO");
        }

        @Test
        @DisplayName("Debería cambiar ENVIADO → ENTREGADO")
        void actualizarEstado_enviadoAEntregado_exitoso() {
            setupCambioEstadoExitoso(1L, "ENVIADO", "ENTREGADO");
            Pedido resultado = sut.actualizarEstado(1L, "ENTREGADO");
            assertThat(resultado.getEstadoDescripcion()).isEqualTo("ENTREGADO");
        }

        @Test
        @DisplayName("Debería lanzar excepción cuando la transición no es válida")
        void actualizarEstado_transicionInvalida_lanzaExcepcion() {
            EstadoPedidoEntity estadoActual = estadoEntity(1L, "PENDIENTE");
            when(pedidoJpaRepo.findByIdWithRelations(1L))
                    .thenReturn(Optional.of(pedidoEntity(1L, estadoActual)));

            assertThatThrownBy(() -> sut.actualizarEstado(1L, "ENTREGADO"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Transición no permitida");

            verify(pedidoJpaRepo, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Debería lanzar excepción cuando ENTREGADO intenta cambiar (estado final)")
        void actualizarEstado_estadoFinalEntregado_lanzaExcepcion() {
            EstadoPedidoEntity estadoFinal = estadoEntity(4L, "ENTREGADO");
            when(pedidoJpaRepo.findByIdWithRelations(1L))
                    .thenReturn(Optional.of(pedidoEntity(1L, estadoFinal)));

            assertThatThrownBy(() -> sut.actualizarEstado(1L, "PENDIENTE"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Transición no permitida");
        }

        @Test
        @DisplayName("Debería lanzar excepción cuando el pedido no existe")
        void actualizarEstado_pedidoInexistente_lanzaExcepcion() {
            when(pedidoJpaRepo.findByIdWithRelations(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sut.actualizarEstado(99L, "PREPARANDO"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Pedido no encontrado");
        }

        @Test
        @DisplayName("Debería restaurar stock de los productos al cancelar")
        void actualizarEstado_cancelar_restauraStock() {
            com.plantopolis.backend.domain.model.Producto producto =
                    com.plantopolis.backend.domain.model.Producto.builder()
                            .idProducto(1L).stock(5).activo(true).build();

            DetallePedidoEntity detalle = DetallePedidoEntity.builder()
                    .idDetalle(1L).idPedido(1L).idProducto(1L).cantidad(3)
                    .precioUnitario(new BigDecimal("25000")).build();

            EstadoPedidoEntity pendiente = estadoEntity(1L, "PENDIENTE");
            PedidoEntity entity = PedidoEntity.builder()
                    .idPedido(1L).idUsuario(10L).idEstado(1L).estado(pendiente)
                    .numeroPedido("PL-TEST").fechaPedido(LocalDateTime.now())
                    .usuario(usuarioEntity()).detalles(List.of(detalle)).build();

            EstadoPedidoEntity cancelado = estadoEntity(5L, "CANCELADO");
            PedidoEntity entityCancelado = pedidoEntity(1L, cancelado);
            Pedido dominioCancelado = pedidoDominio(1L, "CANCELADO");

            when(pedidoJpaRepo.findByIdWithRelations(1L))
                    .thenReturn(Optional.of(entity))
                    .thenReturn(Optional.of(entityCancelado));
            when(estadoPedidoRepo.findByDescripcionEstado("CANCELADO"))
                    .thenReturn(Optional.of(cancelado));
            when(pedidoJpaRepo.saveAndFlush(any())).thenReturn(entityCancelado);
            when(pedidoMapper.toDomain(entityCancelado)).thenReturn(dominioCancelado);
            when(productoRepository.buscarPorId(1L)).thenReturn(Optional.of(producto));
            when(productoRepository.guardar(any())).thenAnswer(i -> i.getArgument(0));
            when(pagoJpaRepo.findByIdPedido(1L)).thenReturn(Optional.empty());

            sut.actualizarEstado(1L, "CANCELADO");

            // stock 5 + 3 cancelados = 8
            org.mockito.ArgumentCaptor<com.plantopolis.backend.domain.model.Producto> cap =
                    org.mockito.ArgumentCaptor.forClass(com.plantopolis.backend.domain.model.Producto.class);
            verify(productoRepository).guardar(cap.capture());
            assertThat(cap.getValue().getStock()).isEqualTo(8);
        }

        @Test
        @DisplayName("NO debería restaurar stock en cambio que no es CANCELADO")
        void actualizarEstado_noRestaurarStockEnOtroEstado() {
            setupCambioEstadoExitoso(1L, "PENDIENTE", "PREPARANDO");
            sut.actualizarEstado(1L, "PREPARANDO");
            verify(productoRepository, never()).guardar(any());
            verify(productoRepository, never()).buscarPorId(any());
        }

        @Test
        @DisplayName("Debería enviar email al cliente al cambiar el estado")
        void actualizarEstado_enviaEmailAlCliente() {
            setupCambioEstadoExitoso(1L, "PENDIENTE", "PREPARANDO");
            sut.actualizarEstado(1L, "PREPARANDO");
            verify(notificacionPort).enviarCambioDeEstado(any(Pedido.class));
        }

        @Test
        @DisplayName("Debería continuar aunque el email falle (silencioso)")
        void actualizarEstado_emailFalla_noLanzaExcepcion() {
            setupCambioEstadoExitoso(1L, "PENDIENTE", "PREPARANDO");
            doThrow(new RuntimeException("SMTP error"))
                    .when(notificacionPort).enviarCambioDeEstado(any());

            assertThatCode(() -> sut.actualizarEstado(1L, "PREPARANDO"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debería llamar entityManager.clear() antes de recargar el pedido")
        void actualizarEstado_llamaClearAntesDeLeerBD() {
            setupCambioEstadoExitoso(1L, "PENDIENTE", "PREPARANDO");
            sut.actualizarEstado(1L, "PREPARANDO");
            verify(entityManager).clear();
        }
    }
}
