package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import com.plantopolis.backend.domain.model.Tarea;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * DTO de respuesta para visualización de tareas operativas en el tablero Kanban y vistas de auditoría.
 */
public record TareaResponse(
        Long idTarea,
        Long idTareaRecurrente,
        String tipoTarea,
        String prioridad,
        String descripcion,
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
        Boolean vencida,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaCompletada
) {
    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");

    public static TareaResponse from(Tarea t) {
        if (t == null) return null;

        boolean esVencida = t.getFechaLimite() != null &&
                t.getFechaLimite().isBefore(LocalDate.now(ZONA_BOGOTA)) &&
                !"COMPLETADA".equalsIgnoreCase(t.getEstadoTarea());

        return new TareaResponse(
                t.getIdTarea(),
                t.getIdTareaRecurrente(),
                t.getTipoTarea(),
                t.getPrioridad(),
                t.getDescripcion(),
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
                esVencida,
                t.getFechaCreacion(),
                t.getFechaCompletada()
        );
    }
}
