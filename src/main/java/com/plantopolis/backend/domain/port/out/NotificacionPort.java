package com.plantopolis.backend.domain.port.out;

import com.plantopolis.backend.domain.model.Pedido;
import com.plantopolis.backend.domain.model.Tarea;

import java.util.List;

/**
 * Puerto de salida para el envío de notificaciones y correos transaccionales del sistema.
 */
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
    boolean enviarAlertaTareasVencidas(String emailDestino, String nombreDestino, List<Tarea> tareasVencidas);

    /**
     * Envía un correo electrónico con el enlace seguro y temporal para restablecer la contraseña (HU2).
     *
     * @param emailDestino Correo del usuario que solicitó la recuperación.
     * @param nombreDestino Nombre del usuario destinatario.
     * @param enlaceRecuperacion URL completa hacia el frontend con el token de reseteo.
     * @return true si el correo fue aceptado por el servicio de mensajería; false en caso contrario.
     */
    boolean enviarRecuperacionPassword(String emailDestino, String nombreDestino, String enlaceRecuperacion);
}