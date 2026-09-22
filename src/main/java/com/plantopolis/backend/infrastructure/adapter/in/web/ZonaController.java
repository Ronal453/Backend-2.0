package com.plantopolis.backend.infrastructure.adapter.in.web;

import com.plantopolis.backend.domain.port.in.ObtenerZonasUseCase;
import com.plantopolis.backend.domain.port.out.LoteRepositoryPort;
import com.plantopolis.backend.domain.port.out.ZonaRepositoryPort;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.ZonaDetalleResponse;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.ZonaOcupacionResponse;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.ZonaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/zonas")
@RequiredArgsConstructor
@Tag(
        name = "Zonas e Invernaderos",
        description = "Consulta de zonas de cultivo disponibles. " +
                      "🔒 Requiere token JWT."
)
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasAnyRole('TRABAJADOR', 'ADMINISTRADOR')")
public class ZonaController {

    private final ObtenerZonasUseCase zonasUseCase;
    private final LoteRepositoryPort loteRepository;
    private final ZonaRepositoryPort zonaRepository;

    @Operation(summary = "Listar zonas activas")
    @GetMapping
    public ResponseEntity<List<ZonaResponse>> listarZonas() {
        var zonas = zonasUseCase.listarZonasActivas().stream()
                .map(ZonaResponse::from)
                .toList();
        return ResponseEntity.ok(zonas);
    }

    @Operation(summary = "Listar zonas activas con ocupación")
    @GetMapping("/ocupacion")
    public ResponseEntity<List<ZonaOcupacionResponse>> listarZonasOcupacion() {
        var zonas = zonasUseCase.listarZonasActivas().stream()
                .map(zona -> ZonaOcupacionResponse.from(
                        zona,
                        loteRepository.contarLotesActivosPorZona(zona.getIdZona()),
                        loteRepository.contarPlantasActivasPorZona(zona.getIdZona())
                ))
                .toList();
        return ResponseEntity.ok(zonas);
    }

    @Operation(summary = "Obtener detalle de una zona activa")
    @GetMapping("/{id}/detalle")
    public ResponseEntity<ZonaDetalleResponse> obtenerDetalleZona(@PathVariable("id") Long idZona) {
        var zona = zonaRepository.buscarPorId(idZona)
                .orElseThrow(() -> new IllegalArgumentException("Zona no encontrada con ID: " + idZona));
        
        var lotesActivos = loteRepository.buscarLotesActivosPorZona(idZona);
        return ResponseEntity.ok(ZonaDetalleResponse.from(zona, lotesActivos));
    }
}
