package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import com.plantopolis.backend.domain.model.Merma;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MermaResponse(
        Long idMerma,
        Long idLote,
        String codigoLote,
        String especieLote,
        Long idCausa,
        String nombreCausa,
        Long idUsuario,
        String nombreUsuario,
        Integer cantidadPerdida,
        LocalDate fechaMerma,
        String observaciones,
        LocalDateTime fechaRegistro
) {
    public static MermaResponse from(Merma m) {
        if (m == null) return null;
        return new MermaResponse(
                m.getIdMerma(),
                m.getIdLote(),
                m.getCodigoLote(),
                m.getEspecieLote(),
                m.getIdCausa(),
                m.getNombreCausa(),
                m.getIdUsuario(),
                m.getNombreUsuario(),
                m.getCantidadPerdida(),
                m.getFechaMerma(),
                m.getObservaciones(),
                m.getFechaRegistro()
        );
    }
}
