package com.plantopolis.backend.infrastructure.adapter.in.web;

import com.plantopolis.backend.domain.port.in.GestionarTareasTrabajadorUseCase;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tareas")
@RequiredArgsConstructor
@Tag(
        name = "Trabajador — Tareas Kanban",
        description = "Operaciones de visualización, transición de estados y comentarios en el tablero Kanban. " +
                      "🔒 Requiere token JWT con rol TRABAJADOR o ADMINISTRADOR."
)
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasAnyRole('TRABAJADOR', 'ADMINISTRADOR')")
public class TrabajadorTareaController {

    private final GestionarTareasTrabajadorUseCase tareasUseCase;

    @Operation(
            summary = "Listar tareas (Kanban)",
            description = "Devuelve las tareas asignadas al trabajador (por defecto) o todas las tareas si soloMias=false."
    )
    @ApiResponse(responseCode = "200", description = "Lista de tareas")
    @GetMapping
    public ResponseEntity<List<TareaResponse>> listarTareas(
            @AuthenticationPrincipal UserDetails user,
            @Parameter(description = "Filtrar únicamente las tareas asignadas al usuario actual (default: true)")
            @RequestParam(defaultValue = "true") boolean soloMias,
            @Parameter(description = "Filtro por estado de la tarea (POR_HACER, EN_PROGRESO, EN_REVISION, COMPLETADA, BLOQUEADA)")
            @RequestParam(required = false) String estado,
            @Parameter(description = "Filtro por prioridad (ALTA, MEDIA, BAJA)")
            @RequestParam(required = false) String prioridad
    ) {
        var tareas = soloMias
                ? tareasUseCase.listarMisTareas(user.getUsername(), estado, prioridad)
                : tareasUseCase.listarTodasLasTareas(estado, prioridad);

        var respuesta = tareas.stream().map(TareaResponse::from).toList();
        return ResponseEntity.ok(respuesta);
    }

    @Operation(summary = "Obtener detalle de una tarea por ID")
    @GetMapping("/{id}")
    public ResponseEntity<TareaResponse> detalleTarea(@PathVariable Long id) {
        var tarea = tareasUseCase.obtenerDetalleTarea(id);
        return ResponseEntity.ok(TareaResponse.from(tarea));
    }

    @Operation(
            summary = "Cambiar estado de una tarea",
            description = "Permite avanzar o retroceder el estado de una tarea validando transiciones permitidas. " +
                          "Opcionalmente permite adjuntar un comentario de avance."
    )
    @PatchMapping("/{id}/estado")
    public ResponseEntity<TareaResponse> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoTareaRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        var actualizada = tareasUseCase.cambiarEstadoTarea(id, request.nuevoEstado(), request.comentario(), user.getUsername());
        return ResponseEntity.ok(TareaResponse.from(actualizada));
    }

    @Operation(summary = "Agregar comentario a una tarea")
    @PostMapping("/{id}/comentarios")
    public ResponseEntity<ComentarioResponse> agregarComentario(
            @PathVariable Long id,
            @Valid @RequestBody ComentarioRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        var comentario = tareasUseCase.agregarComentario(id, request.comentario(), user.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(ComentarioResponse.from(comentario));
    }

    @Operation(summary = "Obtener comentarios de una tarea")
    @GetMapping("/{id}/comentarios")
    public ResponseEntity<List<ComentarioResponse>> obtenerComentarios(@PathVariable Long id) {
        var comentarios = tareasUseCase.obtenerComentariosTarea(id).stream()
                .map(ComentarioResponse::from)
                .toList();
        return ResponseEntity.ok(comentarios);
    }

    @Operation(summary = "Obtener historial de cambios de una tarea")
    @GetMapping("/{id}/historial")
    public ResponseEntity<List<HistorialTareaResponse>> obtenerHistorial(@PathVariable Long id) {
        var historial = tareasUseCase.obtenerHistorialTarea(id).stream()
                .map(HistorialTareaResponse::from)
                .toList();
        return ResponseEntity.ok(historial);
    }
}
