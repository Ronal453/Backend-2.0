package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import com.plantopolis.backend.domain.model.ComentarioTarea;

import java.time.LocalDateTime;

public record ComentarioResponse(
        Long idComentario,
        Long idTarea,
        Long idUsuario,
        String nombreUsuario,
        String comentario,
        LocalDateTime fechaCreacion
) {
    public static ComentarioResponse from(ComentarioTarea c) {
        if (c == null) return null;
        return new ComentarioResponse(
                c.getIdComentario(),
                c.getIdTarea(),
                c.getIdUsuario(),
                c.getNombreUsuario(),
                c.getComentario(),
                c.getFechaCreacion()
        );
    }
}
