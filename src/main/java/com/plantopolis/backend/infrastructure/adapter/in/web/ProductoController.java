package com.plantopolis.backend.infrastructure.adapter.in.web;

import com.plantopolis.backend.domain.port.in.ObtenerProductosUseCase;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.CategoriaResponse;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.ProductoResponse;
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
public class ProductoController {

    private final ObtenerProductosUseCase productoUseCase;

    /**
     * GET /api/productos
     * Parámetros opcionales:
     *   nombre, idCategoria, idTipo, precioMin, precioMax
     *   page (default 0), size (default 20), sort (default precio)
     */
    @GetMapping
    public ResponseEntity<Page<ProductoResponse>> listar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Long idCategoria,
            @RequestParam(required = false) Long idTipo,
            @RequestParam(required = false) BigDecimal precioMin,
            @RequestParam(required = false) BigDecimal precioMax,
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "20")  int size,
            @RequestParam(defaultValue = "precio") String sort
    ) {
        var pageable = PageRequest.of(page, size, Sort.by(sort).ascending());
        var resultado = productoUseCase
                .listarProductos(nombre, idCategoria, idTipo, precioMin, precioMax, pageable)
                .map(ProductoResponse::from);
        return ResponseEntity.ok(resultado);
    }

    /**
     * GET /api/productos/{id}
     * Detalle completo de un producto
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponse> detalle(@PathVariable Long id) {
        var producto = productoUseCase.obtenerDetalle(id);
        return ResponseEntity.ok(ProductoResponse.from(producto));
    }

    /**
     * GET /api/productos/categorias
     * Lista todas las categorías disponibles
     */
    @GetMapping("/categorias")
    public ResponseEntity<List<CategoriaResponse>> categorias() {
        var lista = productoUseCase.listarCategorias()
                .stream()
                .map(CategoriaResponse::from)
                .toList();
        return ResponseEntity.ok(lista);
    }
}