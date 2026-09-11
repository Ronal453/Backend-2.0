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

import java.util.List;

@RestController
@RequestMapping("/api/admin/productos")
@RequiredArgsConstructor
@Tag(name = "Admin — Inventario",
     description = "Gestión de inventario de productos. 🔒 Requiere rol ADMINISTRADOR.")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminProductoController {

    private final GestionarProductosAdminUseCase adminProductoUseCase;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Page<ProductoResponse>> listar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Long idCategoria,
            @RequestParam(required = false) Long idTipo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "nombreProducto") String sort
    ) {
        var pageable = PageRequest.of(page, size, Sort.by(sort).ascending());
        return ResponseEntity.ok(
                adminProductoUseCase.listarTodos(nombre, idCategoria, idTipo, pageable)
                        .map(ProductoResponse::from)
        );
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoAdminRequest request) {
        var producto = mapRequestToDomain(request);
        var creado = adminProductoUseCase.crear(producto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductoResponse.from(creado));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ProductoResponse> actualizar(
            @PathVariable Long id, @Valid @RequestBody ProductoAdminRequest request) {
        var datos = mapRequestToDomain(request);
        var actualizado = adminProductoUseCase.actualizar(id, datos);
        return ResponseEntity.ok(ProductoResponse.from(actualizado));
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ProductoResponse> activar(@PathVariable Long id) {
        return ResponseEntity.ok(ProductoResponse.from(adminProductoUseCase.activar(id)));
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ProductoResponse> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(ProductoResponse.from(adminProductoUseCase.desactivar(id)));
    }

    @GetMapping("/stock-critico")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<ProductoResponse>> listarStockCritico() {
        var productos = adminProductoUseCase.listarStockCritico()
                .stream().map(ProductoResponse::from).toList();
        return ResponseEntity.ok(productos);
    }

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
                .stockMinimoAlerta(req.stockMinimoAlerta())
                .build();
    }
}