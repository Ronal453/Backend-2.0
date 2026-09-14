package com.plantopolis.backend.domain.port.out;

import com.plantopolis.backend.domain.model.HistorialTarea;

import java.util.List;

public interface HistorialTareaRepositoryPort {
    HistorialTarea guardar(HistorialTarea historial);
    List<HistorialTarea> buscarPorIdTarea(Long idTarea);
}
