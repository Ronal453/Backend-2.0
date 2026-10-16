package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import com.plantopolis.backend.domain.model.TareaRecurrente;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Respuesta con el detalle de una definición de tarea recurrente (HU23).
 */
public record TareaRecurrenteResponse(
        Long idTareaRecurrente,
        String tipoTarea,
        String prioridad,
        String descripcion,
        Integer frecuenciaDias,
        Long idZona,
        String nombreZona,
        Long idLote,
        String codigoLote,
        Long idTrabajadorAsignado,
        String nombreTrabajador,
        String emailTrabajador,
        Boolean activo,
        LocalDate fechaProximaEjecucion,
        LocalDateTime fechaCreacion
) {
    public static TareaRecurrenteResponse from(TareaRecurrente r) {
        if (r == null) return null;
        return new TareaRecurrenteResponse(
                r.getIdTareaRecurrente(),
                r.getTipoTarea(),
                r.getPrioridad(),
                r.getDescripcion(),
                r.getFrecuenciaDias(),
                r.getIdZona(),
                r.getNombreZona(),
                r.getIdLote(),
                r.getCodigoLote(),
                r.getIdTrabajadorAsignado(),
                r.getNombreTrabajador(),
                r.getEmailTrabajador(),
                r.getActivo(),
                r.getFechaProximaEjecucion(),
                r.getFechaCreacion()
        );
    }
}
