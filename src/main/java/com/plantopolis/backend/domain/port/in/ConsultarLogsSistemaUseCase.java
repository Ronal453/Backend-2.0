package com.plantopolis.backend.domain.port.in;

import com.plantopolis.backend.domain.model.LogSistema;
import com.plantopolis.backend.domain.model.ResumenLogsSistema;

/**
 * Puerto de entrada (Caso de Uso) para la consulta, filtrado y registro de logs del sistema.
 * Permite al administrador supervisar todas las acciones que ejecuta el sistema.
 */
public interface ConsultarLogsSistemaUseCase {

    /**
     * Consulta los logs del sistema aplicando filtros opcionales de búsqueda, módulo,
     * nivel de severidad y origen, devolviendo también las métricas consolidadas.
     *
     * @param busqueda Texto libre para filtrar por acción, descripción, usuario o endpoint.
     * @param modulo   Módulo del sistema (ej. TAREAS, LOTES, PEDIDOS, AUTH, BACKUP).
     * @param nivel    Nivel de log (INFO, SUCCESS, WARN, ERROR).
     * @param origen   Fuente del log (API_HTTP, AUDITORIA_BD, MOTOR_SISTEMA).
     * @param limite   Cantidad máxima de registros a retornar.
     * @return Objeto {@link ResumenLogsSistema} con KPIs y lista de eventos ordenados.
     */
    ResumenLogsSistema consultarLogs(String busqueda, String modulo, String nivel, String origen, int limite);

    /**
     * Registra en tiempo real una nueva acción ejecutada por el sistema.
     *
     * @param log Evento de actividad a registrar.
     */
    void registrarEvento(LogSistema log);

    /**
     * Limpia el buffer de eventos transitorios en memoria y registra la acción de limpieza.
     *
     * @param usuarioAdmin Correo o nombre del administrador que ejecuta la limpieza.
     */
    void limpiarLogsEnMemoria(String usuarioAdmin);
}
