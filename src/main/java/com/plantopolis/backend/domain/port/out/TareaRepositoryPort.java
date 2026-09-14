package com.plantopolis.backend.domain.port.out;

import com.plantopolis.backend.domain.model.Tarea;

import java.util.List;
import java.util.Optional;

public interface TareaRepositoryPort {
    List<Tarea> buscarPorTrabajador(Long idTrabajador, String estado, String prioridad);
    List<Tarea> buscarTodas(String estado, String prioridad);
    Optional<Tarea> buscarPorId(Long idTarea);
    Tarea guardar(Tarea tarea);
}
