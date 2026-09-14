package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import com.plantopolis.backend.domain.model.HistorialTarea;

import java.time.LocalDateTime;

public record HistorialTareaResponse(
        Long idHistorialTarea,
        Long idTarea,
        Long idUsuario,
        String nombreUsuario,
        String tipoCambio,
        String estadoAnterior,
        String estadoNuevo,
        Long idTrabajadorAnterior,
        String nombreTrabajadorAnterior,
        Long idTrabajadorNuevo,
        String nombreTrabajadorNuevo,
        LocalDateTime fechaCambio
) {
    public static HistorialTareaResponse from(HistorialTarea h) {
        if (h == null) return null;
        return new HistorialTareaResponse(
                h.getIdHistorialTarea(),
                h.getIdTarea(),
                h.getIdUsuario(),
                h.getNombreUsuario(),
                h.getTipoCambio(),
                h.getEstadoAnterior(),
                h.getEstadoNuevo(),
                h.getIdTrabajadorAnterior(),
                h.getNombreTrabajadorAnterior(),
                h.getIdTrabajadorNuevo(),
                h.getNombreTrabajadorNuevo(),
                h.getFechaCambio()
        );
    }
}
