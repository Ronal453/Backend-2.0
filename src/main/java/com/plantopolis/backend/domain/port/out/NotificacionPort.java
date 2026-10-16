package com.plantopolis.backend.domain.port.out;

import com.plantopolis.backend.domain.model.Pedido;

public interface NotificacionPort {

    /** Email de confirmación al crear el pedido */
    void enviarConfirmacionPedido(Pedido pedido);

    /** Email cuando el admin cambia el estado del pedido */
    void enviarCambioDeEstado(Pedido pedido);

    /**
     * Envía alerta consolidada por correo electrónico al administrador con el listado
     * de tareas que superaron su fecha límite sin ser completadas (HU28).
     *
     * @param emailDestino Correo del administrador.
     * @param nombreDestino Nombre del destinatario.
     * @param tareasVencidas Lista de tareas vencidas.
     * @return true si el envío fue exitoso; false si ocurrió una falla en el transporte de email.
     */
    boolean enviarAlertaTareasVencidas(String emailDestino, String nombreDestino, java.util.List<com.plantopolis.backend.domain.model.Tarea> tareasVencidas);
}