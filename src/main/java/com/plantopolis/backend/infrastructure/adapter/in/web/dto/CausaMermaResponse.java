package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import com.plantopolis.backend.domain.model.CausaMerma;

public record CausaMermaResponse(
        Long idCausa,
        String nombreCausa
) {
    public static CausaMermaResponse from(CausaMerma c) {
        if (c == null) return null;
        return new CausaMermaResponse(
                c.getIdCausa(),
                c.getNombreCausa()
        );
    }
}
