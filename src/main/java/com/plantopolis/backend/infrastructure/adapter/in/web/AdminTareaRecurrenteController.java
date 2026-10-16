package com.plantopolis.backend.infrastructure.adapter.in.web;

import com.plantopolis.backend.domain.model.TareaRecurrente;
import com.plantopolis.backend.domain.port.in.GestionarTareasRecurrentesUseCase;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.CrearTareaRecurrenteRequest;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.TareaRecurrenteResponse;
import io.swagger.v3.oas.annotations.Operation;
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

/**
 * Controlador REST para la configuración y administración de tareas periódicas y recurrentes (HU23).
 */
@RestController
@RequestMapping("/api/admin/tareas-recurrentes")
@RequiredArgsConstructor
@Tag(
        name = "Admin — Tareas Recurrentes",
        description = "Configuración de reglas automáticas de generación periódica de tareas. " +
                      "🔒 Requiere token JWT con rol ADMINISTRADOR."
)
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminTareaRecurrenteController {

    private final GestionarTareasRecurrentesUseCase recurrentesUseCase;

    @Operation(summary = "Crear regla de tarea recurrente (HU23)")
    @ApiResponse(responseCode = "201", description = "Regla recurrente creada exitosamente")
    @PostMapping
    public ResponseEntity<TareaRecurrenteResponse> crear(
            @Valid @RequestBody CrearTareaRecurrenteRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        var definicion = TareaRecurrente.builder()
                .tipoTarea(request.tipoTarea())
                .prioridad(request.prioridad())
                .descripcion(request.descripcion())
                .frecuenciaDias(request.frecuenciaDias())
                .idZona(request.idZona())
                .idLote(request.idLote())
                .idTrabajadorAsignado(request.idTrabajadorAsignado())
                .build();

        var guardada = recurrentesUseCase.crear(definicion, request.fechaInicio(), user.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(TareaRecurrenteResponse.from(guardada));
    }

    @Operation(summary = "Listar todas las tareas recurrentes configuradas")
    @GetMapping
    public ResponseEntity<List<TareaRecurrenteResponse>> listar() {
        var lista = recurrentesUseCase.listar().stream()
                .map(TareaRecurrenteResponse::from)
                .toList();
        return ResponseEntity.ok(lista);
    }

    @Operation(summary = "Obtener detalle de una tarea recurrente")
    @GetMapping("/{id}")
    public ResponseEntity<TareaRecurrenteResponse> detalle(@PathVariable Long id) {
        var regla = recurrentesUseCase.buscarPorId(id);
        return ResponseEntity.ok(TareaRecurrenteResponse.from(regla));
    }

    @Operation(summary = "Actualizar configuración de tarea recurrente")
    @PutMapping("/{id}")
    public ResponseEntity<TareaRecurrenteResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody CrearTareaRecurrenteRequest request
    ) {
        var cambios = TareaRecurrente.builder()
                .tipoTarea(request.tipoTarea())
                .prioridad(request.prioridad())
                .descripcion(request.descripcion())
                .frecuenciaDias(request.frecuenciaDias())
                .idZona(request.idZona())
                .idLote(request.idLote())
                .idTrabajadorAsignado(request.idTrabajadorAsignado())
                .build();

        var actualizada = recurrentesUseCase.actualizar(id, cambios);
        return ResponseEntity.ok(TareaRecurrenteResponse.from(actualizada));
    }

    @Operation(summary = "Activar tarea recurrente")
    @PatchMapping("/{id}/activar")
    public ResponseEntity<TareaRecurrenteResponse> activar(@PathVariable Long id) {
        var actualizada = recurrentesUseCase.cambiarActivo(id, true);
        return ResponseEntity.ok(TareaRecurrenteResponse.from(actualizada));
    }

    @Operation(summary = "Desactivar / pausar tarea recurrente")
    @PatchMapping({"/{id}/desactivar", "/{id}/pausar"})
    public ResponseEntity<TareaRecurrenteResponse> desactivar(@PathVariable Long id) {
        var actualizada = recurrentesUseCase.cambiarActivo(id, false);
        return ResponseEntity.ok(TareaRecurrenteResponse.from(actualizada));
    }

    @Operation(summary = "Generar instancias pendientes de tareas recurrentes para hoy (HU23)")
    @PostMapping({"/generar-pendientes", "/procesos/generar-pendientes"})
    public ResponseEntity<com.plantopolis.backend.infrastructure.adapter.in.web.dto.ResultadoProcesoResponse> generarPendientes() {
        int generadas = recurrentesUseCase.generarInstanciasPendientes(java.time.LocalDate.now(java.time.ZoneId.of("America/Bogota")));
        return ResponseEntity.ok(new com.plantopolis.backend.infrastructure.adapter.in.web.dto.ResultadoProcesoResponse(
                "Proceso ejecutado. Nuevas instancias generadas: " + generadas,
                generadas
        ));
    }
}
