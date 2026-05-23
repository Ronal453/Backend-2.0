package com.plantopolis.backend.infrastructure.adapter.in.web;

import com.plantopolis.backend.domain.model.Producto;
import com.plantopolis.backend.domain.port.in.GestionarProductosAdminUseCase;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.ProductoAdminRequest;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.ProductoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para la gestión de inventario de productos (admin).
 *
 *
 * Todos los endpoints requieren:
 *   - Token JWT válido (Bearer Authentication)
 *   - Rol ADMINISTRADOR (@PreAuthorize)
 *
 * Endpoints:
 *   GET    /api/admin/productos         → listar todos (con filtros)
 *   POST   /api/admin/productos         → crear producto
 *   PUT    /api/admin/productos/{id}    → actualizar producto
 *   PATCH  /api/admin/productos/{id}/activar    → activar producto
 *   PATCH  /api/admin/productos/{id}/desactivar → desactivar producto
 */
@RestController
@RequestMapping("/api/admin/productos")
@RequiredArgsConstructor
@Tag(
    name = "Admin — Inventario",
    description = "Gestión de inventario de productos. " +
                  "🔒 Requiere token JWT + rol ADMINISTRADOR."
)
@SecurityRequirement(name = "Bearer Authentication")
public class AdminProductoController {

    private final GestionarProductosAdminUseCase adminProductoUseCase;

    // ── LISTAR TODOS (con filtros) ─────────────────────────────────────────
    @Operation(
        summary = "Listar todos los productos (admin)",
        description = "Devuelve TODOS los productos, activos e inactivos. " +
                      "A diferencia del catálogo público, el admin ve todo."
    )
    @ApiResponse(responseCode = "200", description = "Lista de productos")
    @ApiResponse(responseCode = "403", description = "Acceso denegado — no es admin")
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")   // solo rol ADMINISTRADOR
    public ResponseEntity<Page<ProductoResponse>> listar(
            @Parameter(description = "Filtro por nombre") @RequestParam(required = false) String nombre,
            @Parameter(description = "Filtro por categoría") @RequestParam(required = false) Long idCategoria,
            @Parameter(description = "Filtro por tipo") @RequestParam(required = false) Long idTipo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "nombreProducto") String sort
    ) {
        // PageRequest construye la paginación con ordenamiento ascendente
        var pageable = PageRequest.of(page, size, Sort.by(sort).ascending());

        // listarTodos() usa buscarTodosAdmin() que incluye inactivos
        return ResponseEntity.ok(
                adminProductoUseCase
                        .listarTodos(nombre, idCategoria, idTipo, pageable)
                        .map(ProductoResponse::from)
        );
    }

    // ── CREAR PRODUCTO ────────────────────────────────────────────────────
    @Operation(
        summary = "Crear nuevo producto",
        description = "Crea un producto nuevo en el sistema. " +
                      "El producto queda activo (visible en catálogo) de inmediato."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Producto creado"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "403", description = "No es admin")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ProductoResponse> crear(
            @Valid @RequestBody ProductoAdminRequest request
    ) {
        // Construir el modelo de dominio desde el DTO de la request
        var producto = mapRequestToDomain(request);

        // Llamar al servicio que persiste el nuevo producto
        var creado = adminProductoUseCase.crear(producto);

        // 201 Created con el producto en el body
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ProductoResponse.from(creado));
    }

    // ── ACTUALIZAR PRODUCTO ───────────────────────────────────────────────
    @Operation(
        summary = "Actualizar producto",
        description = "Actualiza los datos de un producto existente. " +
                      "Solo los campos enviados se modifican."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Producto actualizado"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "409", description = "Producto no encontrado"),
        @ApiResponse(responseCode = "403", description = "No es admin")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ProductoResponse> actualizar(
            @Parameter(description = "ID del producto a actualizar", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody ProductoAdminRequest request
    ) {
        // Construir dominio desde DTO y llamar al servicio
        var datos = mapRequestToDomain(request);
        var actualizado = adminProductoUseCase.actualizar(id, datos);
        return ResponseEntity.ok(ProductoResponse.from(actualizado));
    }

    // ── ACTIVAR PRODUCTO ──────────────────────────────────────────────────
    @Operation(
        summary = "Activar producto",
        description = "Pone el producto en estado activo = true. " +
                      "A partir de este momento aparece en el catálogo público."
    )
    @ApiResponse(responseCode = "200", description = "Producto activado")
    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ProductoResponse> activar(
            @Parameter(description = "ID del producto", example = "1")
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                ProductoResponse.from(adminProductoUseCase.activar(id))
        );
    }

    // ── DESACTIVAR PRODUCTO ───────────────────────────────────────────────
    @Operation(
        summary = "Desactivar producto",
        description = "Pone el producto en estado activo = false. " +
                      "Desaparece del catálogo público pero permanece en BD."
    )
    @ApiResponse(responseCode = "200", description = "Producto desactivado")
    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ProductoResponse> desactivar(
            @Parameter(description = "ID del producto", example = "1")
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                ProductoResponse.from(adminProductoUseCase.desactivar(id))
        );
    }

    // ── Helper: mapear request DTO → modelo de dominio ─────────────────────
    /**
     * Convierte un ProductoAdminRequest en un Producto de dominio.
     * Se llama desde crear() y actualizar() para no repetir el mapeo.
     */
    private Producto mapRequestToDomain(ProductoAdminRequest req) {
        return Producto.builder()
                .nombreProducto(req.nombreProducto())
                .descripcion(req.descripcion())
                .precio(req.precio())
                .stock(req.stock())
                .imagenUrl(req.imagenUrl())
                .idCategoria(req.idCategoria())
                .idTipo(req.idTipo())
                .cuidados(req.cuidados())
                .luz(req.luz())
                .riego(req.riego())
                .tamanioEstimado(req.tamanioEstimado())
                .build();
    }
}