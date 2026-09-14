package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import com.plantopolis.backend.domain.model.Zona;

public record ZonaResponse(
        Long idZona,
        String nombre,
        Integer capacidadMaxima,
        String tipoCondicion,
        String exposicionSolar
) {
    public static ZonaResponse from(Zona z) {
        if (z == null) return null;
        return new ZonaResponse(
                z.getIdZona(),
                z.getNombre(),
                z.getCapacidadMaxima(),
                z.getTipoCondicion(),
                z.getExposicionSolar()
        );
    }
}
