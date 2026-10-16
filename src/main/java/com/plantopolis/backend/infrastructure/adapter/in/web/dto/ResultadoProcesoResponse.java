package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

/**
 * DTO de respuesta para ejecuciones manuales o automáticas de procesos por lotes.
 */
public record ResultadoProcesoResponse(
        String mensaje,
        int cantidadProcesada
) {}
