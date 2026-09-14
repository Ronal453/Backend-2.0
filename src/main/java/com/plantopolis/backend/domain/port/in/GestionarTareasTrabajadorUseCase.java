package com.plantopolis.backend.domain.port.in;

import com.plantopolis.backend.domain.model.ComentarioTarea;
import com.plantopolis.backend.domain.model.HistorialTarea;
import com.plantopolis.backend.domain.model.Tarea;

import java.util.List;

public interface GestionarTareasTrabajadorUseCase {
    List<Tarea> listarMisTareas(String emailTrabajador, String estado, String prioridad);
    List<Tarea> listarTodasLasTareas(String estado, String prioridad);
    Tarea obtenerDetalleTarea(Long idTarea);
    Tarea cambiarEstadoTarea(Long idTarea, String nuevoEstado, String comentario, String emailUsuario);
    ComentarioTarea agregarComentario(Long idTarea, String comentario, String emailUsuario);
    List<HistorialTarea> obtenerHistorialTarea(Long idTarea);
    List<ComentarioTarea> obtenerComentariosTarea(Long idTarea);
}
