package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.*;
import com.plantopolis.backend.domain.port.in.GestionarTareasTrabajadorUseCase;
import com.plantopolis.backend.domain.port.out.ComentarioTareaRepositoryPort;
import com.plantopolis.backend.domain.port.out.HistorialTareaRepositoryPort;
import com.plantopolis.backend.domain.port.out.TareaRepositoryPort;
import com.plantopolis.backend.domain.port.out.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

/**
 * Servicio de aplicación para las operaciones de visualización, transición y comentarios
 * del tablero Kanban por parte del personal operativo del vivero (HU24, HU25, HU26, HU27).
 */
@Service
@RequiredArgsConstructor
public class TrabajadorTareaService implements GestionarTareasTrabajadorUseCase {

    private final TareaRepositoryPort tareaRepo;
    private final HistorialTareaRepositoryPort historialRepo;
    private final ComentarioTareaRepositoryPort comentarioRepo;
    private final UsuarioRepositoryPort usuarioRepo;

    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");
    private static final Long ROL_ADMINISTRADOR = 1L;

    @Override
    public List<Tarea> listarMisTareas(String emailTrabajador, String estado, String prioridad) {
        var usuario = usuarioRepo.buscarPorEmail(emailTrabajador)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con email: " + emailTrabajador));
        return tareaRepo.buscarPorTrabajador(usuario.getIdUsuario(), normalizar(estado), normalizar(prioridad));
    }

    @Override
    public List<Tarea> listarTodasLasTareas(String estado, String prioridad) {
        return tareaRepo.buscarTodas(normalizar(estado), normalizar(prioridad));
    }

    @Override
    public Tarea obtenerDetalleTarea(Long idTarea) {
        return tareaRepo.buscarPorId(idTarea)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tarea no encontrada con ID: " + idTarea));
    }

    @Override
    @Transactional
    public Tarea cambiarEstadoTarea(Long idTarea, String nuevoEstadoStr, String comentario, String emailUsuario) {
        var tarea = obtenerDetalleTarea(idTarea);
        var usuario = usuarioRepo.buscarPorEmail(emailUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con email: " + emailUsuario));

        // Validación de permisos de propiedad (HU25):
        // El TRABAJADOR solo puede mover tareas asignadas a su propia cuenta.
        // El ADMINISTRADOR puede mover cualquier tarea operativa.
        boolean esAdmin = ROL_ADMINISTRADOR.equals(usuario.getIdRol()) ||
                "ADMINISTRADOR".equalsIgnoreCase(usuario.getRolNombre());

        if (!esAdmin) {
            if (!Objects.equals(tarea.getIdTrabajadorAsignado(), usuario.getIdUsuario())) {
                throw new OperacionNoPermitidaException("No tienes permisos para modificar el estado de una tarea asignada a otro trabajador o en backlog.");
            }
        }

        EstadoTarea estadoActual = EstadoTarea.desde(tarea.getEstadoTarea());
        EstadoTarea estadoDestino = EstadoTarea.desde(nuevoEstadoStr);

        // Si ya está en el estado destino, no hace nada
        if (estadoActual == estadoDestino) {
            return tarea;
        }

        // Validación de máquina de estados de negocio (HU25 / Decisión acordada)
        if (!estadoActual.puedeTransicionarA(estadoDestino)) {
            throw new IllegalStateException(
                    "Transición no permitida de '" + estadoActual + "' a '" + estadoDestino +
                    "'. Estados destino válidos desde " + estadoActual + ": " + estadoActual.destinosPermitidos()
            );
        }

        tarea.setEstadoTarea(estadoDestino.name());
        if (EstadoTarea.COMPLETADA == estadoDestino) {
            tarea.setFechaCompletada(LocalDateTime.now(ZONA_BOGOTA));
        }

        var tareaActualizada = tareaRepo.guardar(tarea);

        // Registro de auditoría (tipoCambio = ESTADO)
        var historial = HistorialTarea.builder()
                .idTarea(idTarea)
                .idUsuario(usuario.getIdUsuario())
                .tipoCambio("ESTADO")
                .estadoAnterior(estadoActual.name())
                .estadoNuevo(estadoDestino.name())
                .fechaCambio(LocalDateTime.now(ZONA_BOGOTA))
                .build();
        historialRepo.guardar(historial);

        // Comentario opcional adjunto en la transición (HU26)
        if (comentario != null && !comentario.trim().isEmpty()) {
            agregarComentario(idTarea, comentario.trim(), emailUsuario);
        }

        return tareaActualizada;
    }

    @Override
    @Transactional
    public ComentarioTarea agregarComentario(Long idTarea, String comentario, String emailUsuario) {
        if (comentario == null || comentario.trim().isEmpty()) {
            throw new IllegalArgumentException("El contenido del comentario no puede estar vacío.");
        }

        var usuario = usuarioRepo.buscarPorEmail(emailUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con email: " + emailUsuario));

        if (tareaRepo.buscarPorId(idTarea).isEmpty()) {
            throw new RecursoNoEncontradoException("Tarea no encontrada con ID: " + idTarea);
        }

        var nuevoComentario = ComentarioTarea.builder()
                .idTarea(idTarea)
                .idUsuario(usuario.getIdUsuario())
                .comentario(comentario.trim())
                .fechaCreacion(LocalDateTime.now(ZONA_BOGOTA))
                .build();

        return comentarioRepo.guardar(nuevoComentario);
    }

    @Override
    public List<HistorialTarea> obtenerHistorialTarea(Long idTarea) {
        return historialRepo.buscarPorIdTarea(idTarea);
    }

    @Override
    public List<ComentarioTarea> obtenerComentariosTarea(Long idTarea) {
        return comentarioRepo.buscarComentariosPorIdTarea(idTarea);
    }

    private String normalizar(String valor) {
        return (valor != null && !valor.isBlank()) ? valor.trim() : null;
    }
}
