package com.plantopolis.backend.infrastructure.adapter.in.web;

import com.plantopolis.backend.domain.port.in.ObtenerReportesUseCase;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.ReporteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/reportes")
@RequiredArgsConstructor
@Tag(name = "Admin — Reportes",
     description = "Dashboard de métricas y reportes de ventas. 🔒 Requiere rol ADMINISTRADOR.")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminReporteController {

    private final ObtenerReportesUseCase reportesUseCase;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ReporteResponse> obtenerReporte() {
        var reporte = reportesUseCase.obtenerReporte();
        return ResponseEntity.ok(ReporteResponse.from(reporte));
    }

    @Operation(summary = "Exportar pedidos a CSV",
               description = "CSV con número, fecha, cliente, correo, estado y total de " +
                              "pedidos dentro del rango de fechas indicado (opcional).")
    @ApiResponse(responseCode = "200", description = "Archivo CSV generado")
    @GetMapping("/exportar-csv")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<byte[]> exportarCsv(
            @Parameter(description = "Fecha inicial (yyyy-MM-dd), inclusive")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,

            @Parameter(description = "Fecha final (yyyy-MM-dd), inclusive")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin
    ) {
        byte[] csv = reportesUseCase.exportarPedidosCsv(fechaInicio, fechaFin);
        String nombreArchivo = "pedidos_plantopolis_" + LocalDate.now() + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}