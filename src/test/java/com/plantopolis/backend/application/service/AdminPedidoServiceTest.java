package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.Pedido;
import com.plantopolis.backend.domain.port.out.NotificacionPort;
import com.plantopolis.backend.domain.port.out.PedidoRepositoryPort;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminPedidoService")
class AdminPedidoServiceTest {

    @Mock private PedidoRepositoryPort      pedidoRepository;
    @Mock private ProductoRepositoryPort    productoRepository;
    @Mock private NotificacionPort          notificacionPort;

    @InjectMocks
    private AdminPedidoService sut;

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

    @Nested
    @DisplayName("listarTodos()")
    class ListarTodosTests {

        @Test
        @DisplayName("Debería retornar página enriquecida")
        void listarTodos_exito() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Pedido> mockPage = new PageImpl<>(List.of(pedidoDominio(1L, "PENDIENTE")));
            when(pedidoRepository.buscarTodosAdmin("PENDIENTE", pageable)).thenReturn(mockPage);

            Page<Pedido> res = sut.listarTodos("PENDIENTE", pageable);

            assertThat(res.getContent()).hasSize(1);
            assertThat(res.getContent().get(0).getEstadoDescripcion()).isEqualTo("PENDIENTE");
        }
    }

    @Nested
    @DisplayName("actualizarEstado()")
    class ActualizarEstadoTests {

        @Test
        @DisplayName("Transición no válida lanza excepción")
        void actualizarEstado_transicionInvalida_lanzaExcepcion() {
            var pedidoMock = pedidoDominio(1L, "PENDIENTE");
            when(pedidoRepository.buscarPorIdEnriquecido(1L)).thenReturn(pedidoMock);

            assertThatThrownBy(() -> sut.actualizarEstado(1L, "ENTREGADO"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Transición no permitida");
        }

        @Test
        @DisplayName("Actualiza estado y envía notificación")
        void actualizarEstado_exito() {
            var pedidoMock = pedidoDominio(1L, "PENDIENTE");
            when(pedidoRepository.buscarPorIdEnriquecido(1L)).thenReturn(pedidoMock);
            
            var pedidoActualizado = pedidoDominio(1L, "EN_PREPARACION");
            when(pedidoRepository.actualizarEstadoPorDescripcion(1L, "EN_PREPARACION"))
                    .thenReturn(pedidoActualizado);

            Pedido res = sut.actualizarEstado(1L, "EN_PREPARACION");

            assertThat(res.getEstadoDescripcion()).isEqualTo("EN_PREPARACION");
            verify(pedidoRepository).actualizarEstadoPorDescripcion(1L, "EN_PREPARACION");
            verify(notificacionPort).enviarCambioDeEstado(pedidoActualizado);
        }

        @Test
        @DisplayName("Silencia error de email pero cambia el estado")
        void actualizarEstado_fallaEmail_retornaExito() {
            var pedidoMock = pedidoDominio(1L, "PENDIENTE");
            when(pedidoRepository.buscarPorIdEnriquecido(1L)).thenReturn(pedidoMock);
            
            var pedidoActualizado = pedidoDominio(1L, "EN_PREPARACION");
            when(pedidoRepository.actualizarEstadoPorDescripcion(1L, "EN_PREPARACION"))
                    .thenReturn(pedidoActualizado);
            doThrow(new RuntimeException("Mail server down"))
                    .when(notificacionPort).enviarCambioDeEstado(any(Pedido.class));

            Pedido res = sut.actualizarEstado(1L, "EN_PREPARACION");

            assertThat(res.getEstadoDescripcion()).isEqualTo("EN_PREPARACION");
            verify(pedidoRepository).actualizarEstadoPorDescripcion(1L, "EN_PREPARACION");
        }
    }
}
