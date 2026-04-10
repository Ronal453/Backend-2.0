package com.plantopolis.backend.domain.port.out;

import com.plantopolis.backend.domain.model.Pedido;
import java.util.List;
import java.util.Optional;

public interface PedidoRepositoryPort {

    Optional<Pedido> buscarPorId(Long idPedido);

    List<Pedido> buscarPorUsuario(Long idUsuario);

    /** Genera un número de pedido único con formato PL-YYYYMMDD-XXXX */
    String generarNumeroPedido();
}