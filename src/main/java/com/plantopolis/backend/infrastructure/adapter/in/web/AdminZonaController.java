package com.plantopolis.backend.infrastructure.adapter.in.web;

import com.plantopolis.backend.domain.port.in.GestionarZonasAdminUseCase;
import com.plantopolis.backend.domain.port.in.ObtenerZonasUseCase;
import com.plantopolis.backend.domain.port.out.LoteRepositoryPort;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.ActualizarZonaRequest;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.CrearZonaRequest;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.ZonaOcupacionResponse;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.ZonaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/zonas")
@RequiredArgsConstructor
@Tag(name = "Admin — Zonas")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminZonaController {

    private final GestionarZonasAdminUseCase gestionarZonasUseCase;
    private final ObtenerZonasUseCase obtenerZonasUseCase;
    private final LoteRepositoryPort loteRepository;

    @Operation(summary = "Listar todas las zonas con ocupación")
    @GetMapping
    public ResponseEntity<List<ZonaOcupacionResponse>> listarTodasZonas() {
        var zonas = obtenerZonasUseCase.listarTodasLasZonas().stream()
                .map(zona -> ZonaOcupacionResponse.from(
                        zona,
                        loteRepository.contarLotesActivosPorZona(zona.getIdZona()),
                        loteRepository.contarPlantasActivasPorZona(zona.getIdZona())
                ))
                .toList();
        return ResponseEntity.ok(zonas);
    }

    @Operation(summary = "Crear zona")
    @PostMapping
    public ResponseEntity<ZonaResponse> crearZona(@Valid @RequestBody CrearZonaRequest request) {
        var zona = gestionarZonasUseCase.crearZona(request.toDomain());
        return ResponseEntity.status(HttpStatus.CREATED).body(ZonaResponse.from(zona));
    }

    @Operation(summary = "Actualizar zona")
    @PutMapping("/{id}")
    public ResponseEntity<ZonaResponse> actualizarZona(
            @PathVariable("id") Long idZona,
            @Valid @RequestBody ActualizarZonaRequest request) {
        boolean confirmar = Boolean.TRUE.equals(request.confirmarReduccionCapacidad());
        var zona = gestionarZonasUseCase.actualizarZona(idZona, request.toDomain(), confirmar);
        return ResponseEntity.ok(ZonaResponse.from(zona));
    }

    @Operation(summary = "Activar zona")
    @PatchMapping("/{id}/activar")
    public ResponseEntity<ZonaResponse> activarZona(@PathVariable("id") Long idZona) {
        var zona = gestionarZonasUseCase.activarZona(idZona);
        return ResponseEntity.ok(ZonaResponse.from(zona));
    }

    @Operation(summary = "Desactivar zona")
    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<ZonaResponse> desactivarZona(@PathVariable("id") Long idZona) {
        var zona = gestionarZonasUseCase.desactivarZona(idZona);
        return ResponseEntity.ok(ZonaResponse.from(zona));
    }
}
