package com.plantopolis.backend.domain.port.out;

import com.plantopolis.backend.domain.model.Pedido;

public interface NotificacionPort {

    /** Email de confirmación al crear el pedido */
    void enviarConfirmacionPedido(Pedido pedido);

    /** Email cuando el admin cambia el estado del pedido */
    void enviarCambioDeEstado(Pedido pedido);
}