package com.plantopolis.backend.domain.port.in;

import com.plantopolis.backend.domain.model.Pedido;
import java.util.List;

public interface ProcesarPedidoUseCase {

    /** Crea el pedido desde el carrito activo del usuario */
    Pedido procesarPedido(String email, Long idMetodoPago, String direccionEnvio);

    /** Historial de pedidos del cliente autenticado */
    List<Pedido> obtenerHistorial(String email);

    /** Detalle de un pedido específico (verifica que pertenezca al usuario) */
    Pedido obtenerDetalle(String email, Long idPedido);
}