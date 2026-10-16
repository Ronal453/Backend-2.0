package com.plantopolis.backend.domain.port.in;

import java.time.LocalDate;

/**
 * Caso de uso primario para la detección y notificación por correo electrónico de tareas vencidas (HU28).
 */
public interface NotificarTareasVencidasUseCase {

    /**
     * Identifica tareas activas cuya fecha límite sea anterior a la fecha actual y que no
     * hayan sido notificadas previamente, enviando una alerta consolidada a los administradores.
     *
     * @param hoy Fecha actual en zona horaria local (Bogotá).
     * @return Cantidad de tareas vencidas alertadas en el proceso.
     */
    int notificarTareasVencidas(LocalDate hoy);
}
