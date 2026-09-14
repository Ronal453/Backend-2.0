package com.plantopolis.backend.domain.port.out;

import com.plantopolis.backend.domain.model.ComentarioTarea;

import java.util.List;

public interface ComentarioTareaRepositoryPort {
    ComentarioTarea guardar(ComentarioTarea comentario);
    List<ComentarioTarea> buscarComentariosPorIdTarea(Long idTarea);
}
