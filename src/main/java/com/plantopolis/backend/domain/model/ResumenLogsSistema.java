package com.plantopolis.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Modelo de dominio que encapsula el listado filtrado de logs junto con las métricas
 * de resumen (KPIs) para el panel administrativo de Logs del Sistema.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenLogsSistema {

    /** Total global de eventos que cumplen con los filtros o disponibles en el sistema. */
    private long totalEventos;

    /** Cantidad de eventos informativos (INFO). */
    private long totalInfo;

    /** Cantidad de acciones completadas exitosamente (SUCCESS). */
    private long totalSuccess;

    /** Cantidad de advertencias o alertas operativas (WARN). */
    private long totalWarn;

    /** Cantidad de errores o fallos de ejecución (ERROR). */
    private long totalError;

    /** Conteo de eventos agrupados por módulo del sistema. */
    private Map<String, Long> eventosPorModulo;

    /** Lista de registros de actividad ordenados del más reciente al más antiguo. */
    private List<LogSistema> logs;
}
