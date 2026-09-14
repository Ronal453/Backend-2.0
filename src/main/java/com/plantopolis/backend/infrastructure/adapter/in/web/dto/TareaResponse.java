package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import com.plantopolis.backend.domain.model.Tarea;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TareaResponse(
        Long idTarea,
        Long idTareaRecurrente,
        String tipoTarea,
        String prioridad,
        String estadoTarea,
        Long idZona,
        String nombreZona,
        Long idLote,
        String codigoLote,
        String especieLote,
        Long idTrabajadorAsignado,
        String nombreTrabajador,
        String emailTrabajador,
        LocalDate fechaLimite,
        Boolean alertaVencidaEnviada,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaCompletada
) {
    public static TareaResponse from(Tarea t) {
        if (t == null) return null;
        return new TareaResponse(
                t.getIdTarea(),
                t.getIdTareaRecurrente(),
                t.getTipoTarea(),
                t.getPrioridad(),
                t.getEstadoTarea(),
                t.getIdZona(),
                t.getNombreZona(),
                t.getIdLote(),
                t.getCodigoLote(),
                t.getEspecieLote(),
                t.getIdTrabajadorAsignado(),
                t.getNombreTrabajador(),
                t.getEmailTrabajador(),
                t.getFechaLimite(),
                t.getAlertaVencidaEnviada(),
                t.getFechaCreacion(),
                t.getFechaCompletada()
        );
    }
}
