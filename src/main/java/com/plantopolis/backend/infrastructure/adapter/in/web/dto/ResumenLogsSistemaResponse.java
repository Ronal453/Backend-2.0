package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import com.plantopolis.backend.domain.model.ResumenLogsSistema;

import java.util.List;
import java.util.Map;

/**
 * DTO de respuesta consolidado que incluye las métricas de resumen (KPIs) y la lista
 * de eventos filtrados para el panel de Logs del Sistema.
 *
 * @param totalEventos     Total de eventos registrados en el sistema.
 * @param totalInfo        Total de eventos informativos.
 * @param totalSuccess     Total de acciones exitosas.
 * @param totalWarn        Total de advertencias.
 * @param totalError       Total de errores.
 * @param eventosPorModulo Conteo de eventos agrupado por módulo.
 * @param logs             Lista de eventos de log filtrados y ordenados cronológicamente.
 */
public record ResumenLogsSistemaResponse(
        long totalEventos,
        long totalInfo,
        long totalSuccess,
        long totalWarn,
        long totalError,
        Map<String, Long> eventosPorModulo,
        List<LogSistemaResponse> logs
) {
    /**
     * Convierte un {@link ResumenLogsSistema} de dominio a su DTO de respuesta REST.
     *
     * @param domain Modelo de dominio consolidado.
     * @return Instancia de {@link ResumenLogsSistemaResponse}.
     */
    public static ResumenLogsSistemaResponse from(ResumenLogsSistema domain) {
        List<LogSistemaResponse> lista = domain.getLogs() != null
                ? domain.getLogs().stream().map(LogSistemaResponse::from).toList()
                : List.of();
        return new ResumenLogsSistemaResponse(
                domain.getTotalEventos(),
                domain.getTotalInfo(),
                domain.getTotalSuccess(),
                domain.getTotalWarn(),
                domain.getTotalError(),
                domain.getEventosPorModulo(),
                lista
        );
    }
}
