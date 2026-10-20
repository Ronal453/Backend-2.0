package com.plantopolis.backend.domain.port.out;

import com.plantopolis.backend.domain.model.LogSistema;

import java.util.List;

/**
 * Puerto de salida para la obtención y almacenamiento de eventos de auditoría y logs del sistema.
 * Abstrae la recolección tanto de tablas históricas de base de datos como del buffer de eventos en vivo.
 */
public interface LogSistemaRepositoryPort {

    /**
     * Obtiene los eventos históricos de auditoría registrados en las tablas operativas y comerciales
     * de la base de datos (tareas, lotes, mermas, pedidos, usuarios).
     *
     * @return Lista de eventos de auditoría convertidos al modelo de dominio {@link LogSistema}.
     */
    List<LogSistema> obtenerLogsAuditoriaBaseDatos();

    /**
     * Obtiene los eventos de actividad y logs capturados en tiempo real durante la ejecución del servidor.
     *
     * @return Lista de eventos en memoria.
     */
    List<LogSistema> obtenerLogsEnTiempoReal();

    /**
     * Guarda un nuevo evento de actividad en el registro en tiempo real.
     *
     * @param log Evento del sistema a almacenar.
     */
    void guardarLogEnTiempoReal(LogSistema log);

    /**
     * Vacía los eventos transitorios almacenados en el buffer de memoria.
     */
    void limpiarBufferTiempoReal();
}
