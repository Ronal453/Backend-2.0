package com.plantopolis.backend.infrastructure.adapter.in.web;

import com.plantopolis.backend.domain.port.in.GestionarLotesTrabajadorUseCase;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.CambiarEstadoLoteRequest;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.HistorialEstadoLoteResponse;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.LoteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lotes")
@RequiredArgsConstructor
@Tag(
        name = "Trabajador — Lotes de Producción",
        description = "Consulta y avance de estado de lotes de cultivo con trazabilidad. " +
                      "🔒 Requiere token JWT con rol TRABAJADOR o ADMINISTRADOR."
)
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasAnyRole('TRABAJADOR', 'ADMINISTRADOR')")
public class TrabajadorLoteController {

    private final GestionarLotesTrabajadorUseCase lotesUseCase;

    @Operation(
            summary = "Listar lotes con filtros",
            description = "Devuelve una página de lotes filtrable por zona y por estado del lote (GERMINANDO, CRECIENDO, LISTO_PARA_VENTA, DESCARTADO)."
    )
    @ApiResponse(responseCode = "200", description = "Página de lotes")
    @GetMapping
    public ResponseEntity<Page<LoteResponse>> listarLotes(
            @Parameter(description = "ID de la zona / invernadero")
            @RequestParam(required = false) Long idZona,
            @Parameter(description = "Estado del lote")
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by("fechaSiembra").descending());
        var lotes = lotesUseCase.listarLotes(idZona, estado, pageable).map(LoteResponse::from);
        return ResponseEntity.ok(lotes);
    }

    @Operation(summary = "Obtener detalle de un lote")
    @GetMapping("/{id}")
    public ResponseEntity<LoteResponse> detalleLote(@PathVariable Long id) {
        var lote = lotesUseCase.obtenerDetalleLote(id);
        return ResponseEntity.ok(LoteResponse.from(lote));
    }

    @Operation(
            summary = "Actualizar estado de un lote",
            description = "Valida transiciones válidas: GERMINANDO -> CRECIENDO -> LISTO_PARA_VENTA o DESCARTADO. " +
                          "Registra la auditoría en el historial de lotes."
    )
    @PatchMapping("/{id}/estado")
    public ResponseEntity<LoteResponse> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoLoteRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        var actualizado = lotesUseCase.cambiarEstadoLote(
                id, request.nuevoEstado(), request.observaciones(), user.getUsername()
        );
        return ResponseEntity.ok(LoteResponse.from(actualizado));
    }

    @Operation(summary = "Obtener historial de estados de un lote")
    @GetMapping("/{id}/historial")
    public ResponseEntity<List<HistorialEstadoLoteResponse>> obtenerHistorial(@PathVariable Long id) {
        var historial = lotesUseCase.obtenerHistorialLote(id).stream()
                .map(HistorialEstadoLoteResponse::from)
                .toList();
        return ResponseEntity.ok(historial);
    }
}
