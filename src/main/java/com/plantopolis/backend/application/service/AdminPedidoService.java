package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.Pedido;
import com.plantopolis.backend.domain.port.in.GestionarPedidosAdminUseCase;
import com.plantopolis.backend.domain.port.out.NotificacionPort;
import com.plantopolis.backend.domain.port.out.PedidoRepositoryPort;
import com.plantopolis.backend.domain.port.out.ProductoRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPedidoService implements GestionarPedidosAdminUseCase {

    private final PedidoRepositoryPort      pedidoRepository;
    private final ProductoRepositoryPort    productoRepository;
    private final NotificacionPort          notificacionPort;

    // Mapa de transiciones válidas entre estados
    private static final Map<String, List<String>> TRANSICIONES_VALIDAS = Map.of(
    "PENDIENTE",       List.of("EN_PREPARACION", "CANCELADO"),
    "EN_PREPARACION",  List.of("ENVIADO",         "CANCELADO"),
    "ENVIADO",         List.of("ENTREGADO",       "CANCELADO"),
    "ENTREGADO",       List.of(),
    "CANCELADO",       List.of()
    );

    // ── Listar todos los pedidos ──────────────────────────────────────────
    @Override
    public Page<Pedido> listarTodos(String estado, Pageable pageable) {
        // PedidoJpaAdapter.buscarTodosAdmin ya incluye el filtrado y el enriquecimiento
        return pedidoRepository.buscarTodosAdmin(estado, pageable);
    }

    // ── Actualizar estado de un pedido ────────────────────────────────────
    @Override
    public Pedido actualizarEstado(Long idPedido, String nuevoEstado) {

        // PASO 1: Cargar el pedido actual desde el dominio
        var pedidoActual = pedidoRepository.buscarPorIdEnriquecido(idPedido);

        String estadoActual = pedidoActual.getEstadoDescripcion() != null
                ? pedidoActual.getEstadoDescripcion()
                : "DESCONOCIDO";

        String nuevoEstadoUpper = nuevoEstado.toUpperCase().trim();

        log.info("Admin: cambiando pedido {} de '{}' a '{}'",
                idPedido, estadoActual, nuevoEstadoUpper);

        // PASO 2: Validar transición
        List<String> permitidos =
                TRANSICIONES_VALIDAS.getOrDefault(estadoActual, List.of());

        if (!permitidos.contains(nuevoEstadoUpper)) {
            throw new RuntimeException(String.format(
                    "Transición no permitida: '%s' → '%s'. Válidos: %s",
                    estadoActual, nuevoEstadoUpper, permitidos));
        }

        // PASO 3: Si es CANCELADO → restaurar stock
        if ("CANCELADO".equals(nuevoEstadoUpper)) {
            restaurarStock(pedidoActual);
        }

        // PASO 4: Persistir el cambio de estado (el puerto se encarga de buscar el ID del estado)
        var pedidoActualizado = pedidoRepository.actualizarEstadoPorDescripcion(idPedido, nuevoEstadoUpper);

        log.info("Pedido {} → estado '{}'", idPedido,
                pedidoActualizado.getEstadoDescripcion());

        // PASO 5: Enviar email al cliente (silencioso si falla)
        try {
            notificacionPort.enviarCambioDeEstado(pedidoActualizado);
            log.info("✅ Email '{}' enviado a: {}",
                    nuevoEstadoUpper, pedidoActualizado.getEmailCliente());
        } catch (Exception e) {
            log.warn("⚠ Email falló (pedido {}): {}", idPedido, e.getMessage());
        }

        return pedidoActualizado;
    }

    // ── restaurarStock: devuelve stock al cancelar ────────────────────────
    private void restaurarStock(Pedido pedido) {
        log.info("Restaurando stock del pedido: {}", pedido.getIdPedido());
        if (pedido.getDetalles() == null) return;

        pedido.getDetalles().forEach(detalle ->
            productoRepository.buscarPorId(detalle.getIdProducto())
                    .ifPresent(producto -> {
                        int stockNuevo = producto.getStock() + detalle.getCantidad();
                        producto.setStock(stockNuevo);
                        productoRepository.guardar(producto);
                        log.debug("Stock restaurado: producto {} → {} ud.",
                                detalle.getIdProducto(), stockNuevo);
                    })
        );
    }
}