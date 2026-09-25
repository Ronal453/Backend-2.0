package com.plantopolis.backend.domain.port.out;

import com.plantopolis.backend.domain.model.Pedido;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PedidoRepositoryPort {

    Optional<Pedido> buscarPorId(Long idPedido);

    List<Pedido> buscarPorUsuario(Long idUsuario);

    String generarNumeroPedido();

    List<Pedido> buscarPorRangoFecha(LocalDateTime fechaInicio, LocalDateTime fechaFin);

    /**
     * Guarda el pedido atómicamente junto con sus detalles y pago.
     */
    Pedido procesarNuevoPedido(Pedido pedido, Long idMetodoPago);

    /**
     * Busca los pedidos paginados para el panel de administración, con filtro de estado opcional.
     * Retorna los pedidos enriquecidos (con pago).
     */
    org.springframework.data.domain.Page<Pedido> buscarTodosAdmin(String estadoFiltro, org.springframework.data.domain.Pageable pageable);

    /**
     * Actualiza el estado de un pedido buscando la entidad EstadoPedido por su descripción.
     */
    Pedido actualizarEstadoPorDescripcion(Long idPedido, String nuevoEstado);

    /**
     * Busca un pedido y lo retorna garantizando que la información del pago está cargada y enriquecida.
     */
    Pedido buscarPorIdEnriquecido(Long idPedido);
}