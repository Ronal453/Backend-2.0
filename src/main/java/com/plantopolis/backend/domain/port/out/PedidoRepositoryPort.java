package com.plantopolis.backend.domain.port.out;

import com.plantopolis.backend.domain.model.Pedido;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PedidoRepositoryPort {

    Optional<Pedido> buscarPorId(Long idPedido);

    List<Pedido> buscarPorUsuario(Long idUsuario);

    String generarNumeroPedido();

    /** Pedidos dentro de un rango de fechas (límites opcionales), para exportar CSV. */
    List<Pedido> buscarPorRangoFecha(LocalDateTime fechaInicio, LocalDateTime fechaFin);
}