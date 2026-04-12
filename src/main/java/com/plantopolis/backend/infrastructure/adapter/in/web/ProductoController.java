package com.plantopolis.backend.infrastructure.adapter.in.web;

import com.plantopolis.backend.domain.port.in.ObtenerProductosUseCase;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.CategoriaResponse;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.ProductoResponse;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.TipoProductoResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
@Tag(
    name = "Catálogo de Productos",
    description = "Endpoints públicos para explorar el catálogo de plantas. " +
                  "No requieren autenticación."
)
public class ProductoController {

    private final ObtenerProductosUseCase productoUseCase;

    // ── LISTAR CON FILTROS ───────────────────────────────────
    @Operation(
        summary = "Listar productos",
        description = "Devuelve una página de productos activos. " +
                      "Todos los filtros son opcionales y combinables."
    )
    


    @SecurityRequirements  // Indica que este endpoint NO requiere JWT
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de productos paginada")
    })
    @GetMapping
    public ResponseEntity<Page<ProductoResponse>> listar(
            @Parameter(description = "Buscar por nombre (parcial, insensible a mayúsculas)")
            @RequestParam(required = false) String nombre,

            @Parameter(description = "Filtrar por ID de categoría")
            @RequestParam(required = false) Long idCategoria,

            @Parameter(description = "Filtrar por ID de tipo de producto")
            @RequestParam(required = false) Long idTipo,

            @Parameter(description = "Precio mínimo")
            @RequestParam(required = false) BigDecimal precioMin,

            @Parameter(description = "Precio máximo")
            @RequestParam(required = false) BigDecimal precioMax,

            @Parameter(description = "Número de página (empieza en 0)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Cantidad de resultados por página")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Campo por el que ordenar: precio, nombreProducto, stock")
            @RequestParam(defaultValue = "precio") String sort
    ) {
        var pageable = PageRequest.of(page, size, Sort.by(sort).ascending());
        var resultado = productoUseCase
                .listarProductos(nombre, idCategoria, idTipo, precioMin, precioMax, pageable)
                .map(ProductoResponse::from);
        return ResponseEntity.ok(resultado);
    }

    // ── DETALLE DE PRODUCTO ──────────────────────────────────
    @Operation(
        summary = "Obtener detalle de un producto",
        description = "Devuelve toda la información de un producto: " +
                      "descripción, cuidados, luz, riego, tamaño estimado e imagen."
    )
    @SecurityRequirements
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Producto encontrado"),
        @ApiResponse(
            responseCode = "409",
            description = "Producto no encontrado",
            content = @Content(schema = @Schema(hidden = true))
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponse> detalle(
            @Parameter(description = "ID del producto", required = true, example = "1")
            @PathVariable Long id) {
        var producto = productoUseCase.obtenerDetalle(id);
        return ResponseEntity.ok(ProductoResponse.from(producto));
    }

    // ── CATEGORÍAS ───────────────────────────────────────────
    @Operation(
        summary = "Listar categorías",
        description = "Devuelve todas las categorías disponibles. " +
                      "Úsalas para filtrar el catálogo."
    )
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "Lista de categorías")
    @GetMapping("/categorias")
    public ResponseEntity<List<CategoriaResponse>> categorias() {
        var lista = productoUseCase.listarCategorias()
                .stream()
                .map(CategoriaResponse::from)
                .toList();
        return ResponseEntity.ok(lista);
    }
        // ── TIPOS ─────────────────────────────────────────────────
    @Operation(
        summary = "Listar tipos de producto",
        description = "Devuelve todos los tipos disponibles: " +
                    "Planta, Semilla, Accesorio, Sustrato."
    )
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "Lista de tipos")
    @GetMapping("/tipos")
    public ResponseEntity<List<TipoProductoResponse>> tipos() {
        var lista = productoUseCase.listarTipos()
                .stream()
                .map(t -> new TipoProductoResponse(t.getIdTipo(), t.getNombreTipo()))
                .toList();
        return ResponseEntity.ok(lista);
    }
}