package com.plantopolis.backend.infrastructure.adapter.in.web;

import com.plantopolis.backend.domain.model.FiltroTareas;
import com.plantopolis.backend.domain.model.Tarea;
import com.plantopolis.backend.domain.port.in.GestionarTareasAdminUseCase;
import com.plantopolis.backend.domain.port.in.GestionarTareasRecurrentesUseCase;
import com.plantopolis.backend.domain.port.in.NotificarTareasVencidasUseCase;
import com.plantopolis.backend.domain.port.out.UsuarioRepositoryPort;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Controlador REST para la gestión administrativa de tareas, reasignaciones y tablero general (HU22, HU24, HU29, HU30).
 */
@RestController
@RequestMapping("/api/admin/tareas")
@RequiredArgsConstructor
@Tag(
        name = "Admin — Tareas de Vivero",
        description = "Programación de tareas, reasignación, visualización global y resumen operativo. " +
                      "🔒 Requiere token JWT con rol ADMINISTRADOR."
)
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminTareaController {

    private final GestionarTareasAdminUseCase adminTareasUseCase;
    private final GestionarTareasRecurrentesUseCase recurrentesUseCase;
    private final NotificarTareasVencidasUseCase notificarVencidasUseCase;
    private final UsuarioRepositoryPort usuarioRepository;

    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");

    @Operation(summary = "Listar trabajadores activos para asignación de tareas (HU22, HU30)")
    @GetMapping("/trabajadores")
    public ResponseEntity<List<UsuarioAdminResponse>> listarTrabajadores() {
        var trabajadores = usuarioRepository.listarActivosPorRol(2L).stream()
                .map(UsuarioAdminResponse::from)
                .toList();
        return ResponseEntity.ok(trabajadores);
    }

    @Operation(summary = "Crear tarea de cuidado (HU22)", description = "Crea una nueva tarea puntual en estado POR_HACER.")
    @ApiResponse(responseCode = "201", description = "Tarea creada exitosamente")
    @PostMapping
    public ResponseEntity<TareaResponse> crearTarea(
            @Valid @RequestBody CrearTareaRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        var nuevaTarea = Tarea.builder()
                .tipoTarea(request.tipoTarea())
                .prioridad(request.prioridad())
                .descripcion(request.descripcion())
                .idZona(request.idZona())
                .idLote(request.idLote())
                .idTrabajadorAsignado(request.idTrabajadorAsignado())
                .fechaLimite(request.fechaLimite())
                .build();

        var guardada = adminTareasUseCase.crearTarea(nuevaTarea, user.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(TareaResponse.from(guardada));
    }

    @Operation(summary = "Listar tareas con filtros para Administrador (HU24)", description = "Devuelve todas las tareas con filtros opcionales.")
    @GetMapping
    public ResponseEntity<List<TareaResponse>> listarTareasAdmin(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String prioridad,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) Long idTrabajador,
            @RequestParam(required = false) Long idAsignadoA,
            @RequestParam(required = false) Boolean sinAsignar,
            @RequestParam(required = false) Boolean soloVencidas,
            @RequestParam(required = false) Long idZona,
            @RequestParam(required = false) Long idLote
    ) {
        Long trabajadorId = idTrabajador != null ? idTrabajador : idAsignadoA;
        var filtro = FiltroTareas.builder()
                .estado(estado)
                .prioridad(prioridad)
                .tipo(tipo)
                .idTrabajador(trabajadorId)
                .sinAsignar(sinAsignar)
                .soloVencidas(soloVencidas)
                .idZona(idZona)
                .idLote(idLote)
                .build();

        var lista = adminTareasUseCase.listarTareasAdmin(filtro).stream()
                .map(TareaResponse::from)
                .toList();

        return ResponseEntity.ok(lista);
    }

    @Operation(summary = "Reasignar tarea a otro trabajador o backlog (HU30)")
    @PatchMapping({"/{id}/asignacion", "/{id}/reasignar"})
    public ResponseEntity<TareaResponse> reasignarTarea(
            @PathVariable Long id,
            @RequestBody ReasignarTareaRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        Long nuevoTrabajador = request != null ? request.resolverIdTrabajador() : null;
        var actualizada = adminTareasUseCase.reasignarTarea(id, nuevoTrabajador, user.getUsername());
        return ResponseEntity.ok(TareaResponse.from(actualizada));
    }

    @Operation(summary = "Resumen y métricas de tareas para Dashboard Operativo (HU29)")
    @GetMapping("/resumen")
    public ResponseEntity<ResumenTareasResponse> obtenerResumen(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin
    ) {
        var resumen = adminTareasUseCase.obtenerResumen(fechaInicio, fechaFin);
        return ResponseEntity.ok(ResumenTareasResponse.from(resumen));
    }

    @Operation(summary = "Disparar proceso manual de generación de recurrentes (HU23)")
    @PostMapping({"/procesos/generar-recurrentes", "/generar-recurrentes"})
    public ResponseEntity<ResultadoProcesoResponse> ejecutarGeneracionRecurrentes() {
        int generadas = recurrentesUseCase.generarInstanciasPendientes(LocalDate.now(ZONA_BOGOTA));
        return ResponseEntity.ok(new ResultadoProcesoResponse(
                "Proceso completado. Se generaron nuevas tareas operativas.",
                generadas
        ));
    }

    @Operation(summary = "Disparar proceso manual de alerta de tareas vencidas (HU28)")
    @PostMapping({"/procesos/alertas-vencidas", "/procesar-alertas-vencidas"})
    public ResponseEntity<ResultadoProcesoResponse> ejecutarAlertaVencidas() {
        int alertadas = notificarVencidasUseCase.notificarTareasVencidas(LocalDate.now(ZONA_BOGOTA));
        return ResponseEntity.ok(new ResultadoProcesoResponse(
                "Proceso de alertas completado. Notificaciones enviadas.",
                alertadas
        ));
    }
}
