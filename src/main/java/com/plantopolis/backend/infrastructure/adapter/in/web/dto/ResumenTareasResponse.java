package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import com.plantopolis.backend.domain.model.ResumenTareas;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * DTO de respuesta para el Dashboard Operativo del Administrador (HU29).
 */
public record ResumenTareasResponse(
        long totalActivas,
        long vencidasActivas,
        long completadasEnPeriodo,
        long sinAsignar,
        Map<String, Long> conteoPorEstado,
        List<ResumenTareas.CargaTrabajador> cargaPorTrabajador,
        List<ResumenTareas.PuntoSerieCompletadas> completadasPorDia,
        List<TareaResponse> tareasVencidas
) {
    public static ResumenTareasResponse from(ResumenTareas r) {
        if (r == null) return null;
        List<TareaResponse> vencidas = (r.getTareasVencidas() != null)
                ? r.getTareasVencidas().stream().map(TareaResponse::from).toList()
                : Collections.emptyList();

        return new ResumenTareasResponse(
                r.getTotalActivas(),
                r.getVencidasActivas(),
                r.getCompletadasEnPeriodo(),
                r.getSinAsignar(),
                r.getConteoPorEstado(),
                r.getCargaPorTrabajador(),
                r.getCompletadasPorDia(),
                vencidas
        );
    }
}
