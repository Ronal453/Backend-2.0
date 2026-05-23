package com.plantopolis.backend.infrastructure.adapter.in.web;

import com.plantopolis.backend.domain.port.in.ObtenerReportesUseCase;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.ReporteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para el dashboard de reportes del panel admin.
 *
 * Endpoint: GET /api/admin/reportes
 *
 * Devuelve en una sola llamada todas las métricas necesarias
 * para construir el dashboard:
 *   - KPI cards (ingresos, pedidos, productos, promedio)
 *   - Distribución de pedidos por estado (gráfica de barras)
 *   - Top 5 productos más vendidos (tabla)
 */
@RestController
@RequestMapping("/api/admin/reportes")
@RequiredArgsConstructor
@Tag(
    name = "Admin — Reportes",
    description = "Dashboard de métricas y reportes de ventas. " +
                  "🔒 Requiere token JWT + rol ADMINISTRADOR."
)
@SecurityRequirement(name = "Bearer Authentication")
public class AdminReporteController {

    private final ObtenerReportesUseCase reportesUseCase;

    // ── OBTENER REPORTE COMPLETO ──────────────────────────────────────────
    @Operation(
        summary = "Obtener métricas del dashboard",
        description = "Calcula y devuelve en tiempo real todas las métricas " +
                      "del dashboard de administración. Incluye: " +
                      "ingresos totales, pedidos por estado y top 5 productos."
    )
    @ApiResponse(responseCode = "200",
                 description = "Reporte calculado correctamente")
    @ApiResponse(responseCode = "403",
                 description = "Acceso denegado — no es administrador")
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ReporteResponse> obtenerReporte() {
        // Delega al servicio que agrega los datos de múltiples repositorios
        var reporte = reportesUseCase.obtenerReporte();

        // Convierte el modelo de dominio al DTO de respuesta
        return ResponseEntity.ok(ReporteResponse.from(reporte));
    }
}