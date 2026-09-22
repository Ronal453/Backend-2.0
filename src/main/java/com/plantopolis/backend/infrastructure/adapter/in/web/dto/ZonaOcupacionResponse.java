package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import com.plantopolis.backend.domain.model.Zona;

public record ZonaOcupacionResponse(
        Long idZona,
        String nombre,
        Integer capacidadMaxima,
        String tipoCondicion,
        String exposicionSolar,
        Boolean activo,
        long lotesActivos,
        long totalPlantas,
        double porcentajeOcupacion,
        boolean alertaSupera90
) {
    public static ZonaOcupacionResponse from(Zona zona, long lotesActivos, long totalPlantas) {
        double porcentaje = (zona.getCapacidadMaxima() != null && zona.getCapacidadMaxima() > 0)
                ? ((double) lotesActivos / zona.getCapacidadMaxima()) * 100
                : 0.0;
        if (porcentaje > 100) porcentaje = 100;

        return new ZonaOcupacionResponse(
                zona.getIdZona(),
                zona.getNombre(),
                zona.getCapacidadMaxima(),
                zona.getTipoCondicion(),
                zona.getExposicionSolar(),
                zona.getActivo(),
                lotesActivos,
                totalPlantas,
                Math.round(porcentaje * 100.0) / 100.0,
                porcentaje >= 90.0
        );
    }
}
