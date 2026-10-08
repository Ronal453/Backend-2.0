package com.plantopolis.backend.infrastructure.adapter.in.web;

import com.plantopolis.backend.domain.port.in.GestionarProveedoresAdminUseCase;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.LoteResponse;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.ProveedorRequest;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.ProveedorResponse;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.ReporteProveedorResponse;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST del módulo de proveedores (HU34 / HU35).
 * <p>
 * Restringido a ADMINISTRADOR tanto por {@code SecurityConfig} (/api/admin/**) como por
 * {@link PreAuthorize}. Las excepciones de negocio se traducen en {@code GlobalExceptionHandler}:
 * IllegalArgumentException → 400, IllegalStateException → 409.
 * </p>
 */
@RestController
@RequestMapping("/api/admin/proveedores")
@RequiredArgsConstructor
@Tag(
        name = "Admin — Proveedores",
        description = "Gestión de proveedores de insumos y trazabilidad de lotes por proveedor. 🔒 Requiere token JWT con rol ADMINISTRADOR."
)
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminProveedorController {

    private final GestionarProveedoresAdminUseCase proveedoresUseCase;

    /**
     * Lista proveedores con filtros opcionales.
     */
    @Operation(summary = "Listar proveedores", description = "Filtra por término (nombre/contacto), tipo de insumo y estado.")
    @GetMapping
    public ResponseEntity<List<ProveedorResponse>> listar(
            @Parameter(description = "Texto a buscar en nombre o contacto") @RequestParam(required = false) String buscar,
            @Parameter(description = "SEMILLAS, SUSTRATOS, MACETAS u OTROS") @RequestParam(required = false) String tipoInsumo,
            @Parameter(description = "true = activos, false = inactivos") @RequestParam(required = false) Boolean activo
    ) {
        var proveedores = proveedoresUseCase.listarProveedores(buscar, tipoInsumo, activo).stream()
                .map(ProveedorResponse::from)
                .toList();
        return ResponseEntity.ok(proveedores);
    }

    /**
     * Proveedores activos, para poblar selectores (p. ej. al registrar lotes).
     * Spring MVC prioriza la ruta literal "/activos" sobre el patrón "/{id}", por lo que no hay ambigüedad.
     */
    @Operation(summary = "Listar proveedores activos")
    @GetMapping("/activos")
    public ResponseEntity<List<ProveedorResponse>> listarActivos() {
        return ResponseEntity.ok(proveedoresUseCase.listarProveedoresActivos().stream()
                .map(ProveedorResponse::from)
                .toList());
    }

    /**
     * HU35: reporte agregado de lotes, plantas y pérdidas por proveedor.
     */
    @Operation(summary = "Reporte de lotes por proveedor",
            description = "Totales de lotes (en cultivo, en tienda, descartados), plantas sembradas/vivas y % de pérdida por proveedor.")
    @GetMapping("/reporte")
    public ResponseEntity<List<ReporteProveedorResponse>> reporte() {
        return ResponseEntity.ok(proveedoresUseCase.generarReporteLotesPorProveedor().stream()
                .map(ReporteProveedorResponse::from)
                .toList());
    }

    /** Detalle de un proveedor. */
    @Operation(summary = "Obtener proveedor")
    @ApiResponse(responseCode = "400", description = "Proveedor no encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<ProveedorResponse> obtener(@PathVariable("id") Long idProveedor) {
        return ResponseEntity.ok(ProveedorResponse.from(proveedoresUseCase.obtenerProveedor(idProveedor)));
    }

    /** HU35: lotes asociados a un proveedor. */
    @Operation(summary = "Lotes de un proveedor", description = "Trazabilidad: lotes cuyo material proviene del proveedor.")
    @GetMapping("/{id}/lotes")
    public ResponseEntity<List<LoteResponse>> lotesDeProveedor(@PathVariable("id") Long idProveedor) {
        return ResponseEntity.ok(proveedoresUseCase.listarLotesPorProveedor(idProveedor).stream()
                .map(LoteResponse::from)
                .toList());
    }

    /** HU34: alta de proveedor. */
    @Operation(summary = "Crear proveedor")
    @ApiResponse(responseCode = "201", description = "Proveedor creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o nombre duplicado")
    @PostMapping
    public ResponseEntity<ProveedorResponse> crear(@Valid @RequestBody ProveedorRequest request) {
        var creado = proveedoresUseCase.crearProveedor(request.toDomain());
        return ResponseEntity.status(HttpStatus.CREATED).body(ProveedorResponse.from(creado));
    }

    /** HU34: edición de proveedor. */
    @Operation(summary = "Actualizar proveedor")
    @PutMapping("/{id}")
    public ResponseEntity<ProveedorResponse> actualizar(
            @PathVariable("id") Long idProveedor,
            @Valid @RequestBody ProveedorRequest request) {
        var actualizado = proveedoresUseCase.actualizarProveedor(idProveedor, request.toDomain());
        return ResponseEntity.ok(ProveedorResponse.from(actualizado));
    }

    /** HU34: activar proveedor. */
    @Operation(summary = "Activar proveedor")
    @PatchMapping("/{id}/activar")
    public ResponseEntity<ProveedorResponse> activar(@PathVariable("id") Long idProveedor) {
        return ResponseEntity.ok(ProveedorResponse.from(proveedoresUseCase.activarProveedor(idProveedor)));
    }

    /** HU34: desactivar proveedor (conserva su historial de lotes). */
    @Operation(summary = "Desactivar proveedor", description = "No elimina datos: los lotes existentes conservan su proveedor.")
    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<ProveedorResponse> desactivar(@PathVariable("id") Long idProveedor) {
        return ResponseEntity.ok(ProveedorResponse.from(proveedoresUseCase.desactivarProveedor(idProveedor)));
    }
}
