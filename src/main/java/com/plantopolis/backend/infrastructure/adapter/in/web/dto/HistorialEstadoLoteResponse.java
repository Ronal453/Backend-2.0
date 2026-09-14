package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import com.plantopolis.backend.domain.model.HistorialEstadoLote;

import java.time.LocalDateTime;

public record HistorialEstadoLoteResponse(
        Long idHistorialLote,
        Long idLote,
        Long idUsuario,
        String nombreUsuario,
        String estadoAnterior,
        String estadoNuevo,
        String observaciones,
        LocalDateTime fechaCambio
) {
    public static HistorialEstadoLoteResponse from(HistorialEstadoLote h) {
        if (h == null) return null;
        return new HistorialEstadoLoteResponse(
                h.getIdHistorialLote(),
                h.getIdLote(),
                h.getIdUsuario(),
                h.getNombreUsuario(),
                h.getEstadoAnterior(),
                h.getEstadoNuevo(),
                h.getObservaciones(),
                h.getFechaCambio()
        );
    }
}
