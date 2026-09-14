package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.ComentarioTarea;
import com.plantopolis.backend.domain.model.HistorialTarea;
import com.plantopolis.backend.domain.model.Tarea;
import com.plantopolis.backend.domain.port.in.GestionarTareasTrabajadorUseCase;
import com.plantopolis.backend.domain.port.out.ComentarioTareaRepositoryPort;
import com.plantopolis.backend.domain.port.out.HistorialTareaRepositoryPort;
import com.plantopolis.backend.domain.port.out.TareaRepositoryPort;
import com.plantopolis.backend.domain.port.out.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TrabajadorTareaService implements GestionarTareasTrabajadorUseCase {

    private final TareaRepositoryPort tareaRepo;
    private final HistorialTareaRepositoryPort historialRepo;
    private final ComentarioTareaRepositoryPort comentarioRepo;
    private final UsuarioRepositoryPort usuarioRepo;

    public static final String ESTADO_POR_HACER = "POR_HACER";
    public static final String ESTADO_EN_PROGRESO = "EN_PROGRESO";
    public static final String ESTADO_EN_REVISION = "EN_REVISION";
    public static final String ESTADO_COMPLETADA = "COMPLETADA";
    public static final String ESTADO_BLOQUEADA = "BLOQUEADA";

    private static final Set<String> ESTADOS_VALIDOS = Set.of(
            ESTADO_POR_HACER, ESTADO_EN_PROGRESO, ESTADO_EN_REVISION, ESTADO_COMPLETADA, ESTADO_BLOQUEADA
    );

    @Override
    public List<Tarea> listarMisTareas(String emailTrabajador, String estado, String prioridad) {
        var usuario = usuarioRepo.buscarPorEmail(emailTrabajador)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + emailTrabajador));
        return tareaRepo.buscarPorTrabajador(usuario.getIdUsuario(), normalizar(estado), normalizar(prioridad));
    }

    @Override
    public List<Tarea> listarTodasLasTareas(String estado, String prioridad) {
        return tareaRepo.buscarTodas(normalizar(estado), normalizar(prioridad));
    }

    @Override
    public Tarea obtenerDetalleTarea(Long idTarea) {
        return tareaRepo.buscarPorId(idTarea)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada con ID: " + idTarea));
    }

    @Override
    @Transactional
    public Tarea cambiarEstadoTarea(Long idTarea, String nuevoEstado, String comentario, String emailUsuario) {
        var tarea = obtenerDetalleTarea(idTarea);
        var usuario = usuarioRepo.buscarPorEmail(emailUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + emailUsuario));

        String estadoActual = tarea.getEstadoTarea();
        String nuevo = nuevoEstado != null ? nuevoEstado.trim().toUpperCase() : "";

        if (!ESTADOS_VALIDOS.contains(nuevo)) {
            throw new RuntimeException("Estado de tarea inválido: " + nuevoEstado);
        }

        if (estadoActual.equalsIgnoreCase(nuevo)) {
            return tarea;
        }

        validarTransicionTarea(estadoActual, nuevo);

        tarea.setEstadoTarea(nuevo);
        if (ESTADO_COMPLETADA.equalsIgnoreCase(nuevo)) {
            tarea.setFechaCompletada(LocalDateTime.now());
        } else if (ESTADO_COMPLETADA.equalsIgnoreCase(estadoActual)) {
            tarea.setFechaCompletada(null);
        }

        var tareaActualizada = tareaRepo.guardar(tarea);

        var historial = HistorialTarea.builder()
                .idTarea(idTarea)
                .idUsuario(usuario.getIdUsuario())
                .tipoCambio("ESTADO")
                .estadoAnterior(estadoActual)
                .estadoNuevo(nuevo)
                .fechaCambio(LocalDateTime.now())
                .build();
        historialRepo.guardar(historial);

        if (comentario != null && !comentario.trim().isEmpty()) {
            agregarComentario(idTarea, comentario.trim(), emailUsuario);
        }

        return tareaActualizada;
    }

    @Override
    @Transactional
    public ComentarioTarea agregarComentario(Long idTarea, String comentario, String emailUsuario) {
        if (comentario == null || comentario.trim().isEmpty()) {
            throw new RuntimeException("El contenido del comentario no puede estar vacío.");
        }

        var usuario = usuarioRepo.buscarPorEmail(emailUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + emailUsuario));

        if (!tareaRepo.buscarPorId(idTarea).isPresent()) {
            throw new RuntimeException("Tarea no encontrada con ID: " + idTarea);
        }

        var nuevoComentario = ComentarioTarea.builder()
                .idTarea(idTarea)
                .idUsuario(usuario.getIdUsuario())
                .comentario(comentario.trim())
                .fechaCreacion(LocalDateTime.now())
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

    private void validarTransicionTarea(String origen, String destino) {
        if (ESTADO_BLOQUEADA.equalsIgnoreCase(destino)) {
            if (ESTADO_COMPLETADA.equalsIgnoreCase(origen)) {
                throw new RuntimeException("Una tarea completada no puede ser bloqueada.");
            }
            return;
        }

        if (ESTADO_BLOQUEADA.equalsIgnoreCase(origen)) {
            if (ESTADO_POR_HACER.equalsIgnoreCase(destino) || ESTADO_EN_PROGRESO.equalsIgnoreCase(destino)) {
                return;
            }
            throw new RuntimeException("Una tarea bloqueada solo puede regresar a 'POR_HACER' o 'EN_PROGRESO'.");
        }

        boolean permitida = false;
        if (ESTADO_POR_HACER.equalsIgnoreCase(origen) && ESTADO_EN_PROGRESO.equalsIgnoreCase(destino)) {
            permitida = true;
        } else if (ESTADO_EN_PROGRESO.equalsIgnoreCase(origen) &&
                (ESTADO_EN_REVISION.equalsIgnoreCase(destino) || ESTADO_POR_HACER.equalsIgnoreCase(destino))) {
            permitida = true;
        } else if (ESTADO_EN_REVISION.equalsIgnoreCase(origen) &&
                (ESTADO_COMPLETADA.equalsIgnoreCase(destino) || ESTADO_EN_PROGRESO.equalsIgnoreCase(destino))) {
            permitida = true;
        } else if (ESTADO_COMPLETADA.equalsIgnoreCase(origen) && ESTADO_EN_REVISION.equalsIgnoreCase(destino)) {
            permitida = true;
        }

        if (!permitida) {
            throw new RuntimeException(
                    "Transición no permitida de " + origen + " a " + destino +
                    ". Flujo permitido: POR_HACER -> EN_PROGRESO -> EN_REVISION -> COMPLETADA (o BLOQUEADA)."
            );
        }
    }

    private String normalizar(String valor) {
        return (valor != null && !valor.isBlank()) ? valor.trim() : null;
    }
}
