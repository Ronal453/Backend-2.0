package com.plantopolis.backend.infrastructure.persistence.mapper;

import com.plantopolis.backend.domain.model.ComentarioTarea;
import com.plantopolis.backend.domain.model.HistorialTarea;
import com.plantopolis.backend.domain.model.Tarea;
import com.plantopolis.backend.infrastructure.persistence.entity.ComentarioTareaEntity;
import com.plantopolis.backend.infrastructure.persistence.entity.HistorialTareaEntity;
import com.plantopolis.backend.infrastructure.persistence.entity.TareaEntity;
import org.springframework.stereotype.Component;

@Component
public class TareaMapper {

    public Tarea toDomain(TareaEntity entity) {
        if (entity == null) return null;
        return Tarea.builder()
                .idTarea(entity.getIdTarea())
                .idTareaRecurrente(entity.getIdTareaRecurrente())
                .tipoTarea(entity.getTipoTarea())
                .prioridad(entity.getPrioridad())
                .estadoTarea(entity.getEstadoTarea())
                .idZona(entity.getIdZona())
                .nombreZona(entity.getZona() != null ? entity.getZona().getNombre() : null)
                .idLote(entity.getIdLote())
                .codigoLote(entity.getLote() != null ? entity.getLote().getCodigoLote() : null)
                .especieLote(entity.getLote() != null ? entity.getLote().getEspecie() : null)
                .idTrabajadorAsignado(entity.getIdTrabajadorAsignado())
                .nombreTrabajador(entity.getTrabajador() != null ? entity.getTrabajador().getNombreCompleto() : null)
                .emailTrabajador(entity.getTrabajador() != null ? entity.getTrabajador().getCorreo() : null)
                .fechaLimite(entity.getFechaLimite())
                .alertaVencidaEnviada(entity.getAlertaVencidaEnviada())
                .fechaCreacion(entity.getFechaCreacion())
                .fechaCompletada(entity.getFechaCompletada())
                .build();
    }

    public TareaEntity toEntity(Tarea domain) {
        if (domain == null) return null;
        return TareaEntity.builder()
                .idTarea(domain.getIdTarea())
                .idTareaRecurrente(domain.getIdTareaRecurrente())
                .tipoTarea(domain.getTipoTarea())
                .prioridad(domain.getPrioridad())
                .estadoTarea(domain.getEstadoTarea())
                .idZona(domain.getIdZona())
                .idLote(domain.getIdLote())
                .idTrabajadorAsignado(domain.getIdTrabajadorAsignado())
                .fechaLimite(domain.getFechaLimite())
                .alertaVencidaEnviada(domain.getAlertaVencidaEnviada() != null ? domain.getAlertaVencidaEnviada() : false)
                .fechaCreacion(domain.getFechaCreacion())
                .fechaCompletada(domain.getFechaCompletada())
                .build();
    }

    public HistorialTarea toDomainHistorial(HistorialTareaEntity entity) {
        if (entity == null) return null;
        return HistorialTarea.builder()
                .idHistorialTarea(entity.getIdHistorialTarea())
                .idTarea(entity.getIdTarea())
                .idUsuario(entity.getIdUsuario())
                .nombreUsuario(entity.getUsuario() != null ? entity.getUsuario().getNombreCompleto() : null)
                .tipoCambio(entity.getTipoCambio())
                .estadoAnterior(entity.getEstadoAnterior())
                .estadoNuevo(entity.getEstadoNuevo())
                .idTrabajadorAnterior(entity.getIdTrabajadorAnterior())
                .nombreTrabajadorAnterior(entity.getTrabajadorAnterior() != null ? entity.getTrabajadorAnterior().getNombreCompleto() : null)
                .idTrabajadorNuevo(entity.getIdTrabajadorNuevo())
                .nombreTrabajadorNuevo(entity.getTrabajadorNuevo() != null ? entity.getTrabajadorNuevo().getNombreCompleto() : null)
                .fechaCambio(entity.getFechaCambio())
                .build();
    }

    public HistorialTareaEntity toEntityHistorial(HistorialTarea domain) {
        if (domain == null) return null;
        return HistorialTareaEntity.builder()
                .idHistorialTarea(domain.getIdHistorialTarea())
                .idTarea(domain.getIdTarea())
                .idUsuario(domain.getIdUsuario())
                .tipoCambio(domain.getTipoCambio())
                .estadoAnterior(domain.getEstadoAnterior())
                .estadoNuevo(domain.getEstadoNuevo())
                .idTrabajadorAnterior(domain.getIdTrabajadorAnterior())
                .idTrabajadorNuevo(domain.getIdTrabajadorNuevo())
                .fechaCambio(domain.getFechaCambio())
                .build();
    }

    public ComentarioTarea toDomainComentario(ComentarioTareaEntity entity) {
        if (entity == null) return null;
        return ComentarioTarea.builder()
                .idComentario(entity.getIdComentario())
                .idTarea(entity.getIdTarea())
                .idUsuario(entity.getIdUsuario())
                .nombreUsuario(entity.getUsuario() != null ? entity.getUsuario().getNombreCompleto() : null)
                .comentario(entity.getComentario())
                .fechaCreacion(entity.getFechaCreacion())
                .build();
    }

    public ComentarioTareaEntity toEntityComentario(ComentarioTarea domain) {
        if (domain == null) return null;
        return ComentarioTareaEntity.builder()
                .idComentario(domain.getIdComentario())
                .idTarea(domain.getIdTarea())
                .idUsuario(domain.getIdUsuario())
                .comentario(domain.getComentario())
                .fechaCreacion(domain.getFechaCreacion())
                .build();
    }
}
