package com.plantopolis.backend.infrastructure.adapter.in.web;

import com.plantopolis.backend.domain.port.in.GestionarCarritoUseCase;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.ActualizarCantidadRequest;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.AgregarItemRequest;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.CarritoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carrito")
@RequiredArgsConstructor
@Tag(
    name = "Carrito de Compras",
    description = "Gestión del carrito del usuario autenticado. " +
                  "🔒 Requiere token JWT."
)
@SecurityRequirement(name = "Bearer Authentication")
public class CarritoController {

    private final GestionarCarritoUseCase carritoUseCase;

    // ── VER CARRITO ──────────────────────────────────────────
    @Operation(
        summary = "Ver mi carrito",
        description = "Devuelve el carrito activo del usuario con todos sus ítems y el total calculado. " +
                      "Si no tiene carrito, crea uno vacío automáticamente."
    )
    @ApiResponse(responseCode = "200",
                 description = "Carrito del usuario",
                 content = @Content(schema = @Schema(implementation = CarritoResponse.class)))
    @GetMapping
    public ResponseEntity<CarritoResponse> verCarrito(
            @AuthenticationPrincipal UserDetails userDetails) {
        var carrito = carritoUseCase.verCarrito(userDetails.getUsername());
        return ResponseEntity.ok(CarritoResponse.from(carrito));
    }

    // ── AGREGAR ÍTEM ─────────────────────────────────────────
    @Operation(
        summary = "Agregar producto al carrito",
        description = "Agrega un producto al carrito. " +
                      "Si el producto ya existe, suma la cantidad. " +
                      "Valida que haya stock suficiente antes de agregar."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Carrito actualizado"),
        @ApiResponse(responseCode = "409",
                     description = "Stock insuficiente o producto no disponible",
                     content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping("/items")
    public ResponseEntity<CarritoResponse> agregarItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AgregarItemRequest request) {
        var carrito = carritoUseCase.agregarItem(
                userDetails.getUsername(),
                request.idProducto(),
                request.cantidad());
        return ResponseEntity.ok(CarritoResponse.from(carrito));
    }

    // ── ACTUALIZAR CANTIDAD ──────────────────────────────────
    @Operation(
        summary = "Actualizar cantidad de un ítem",
        description = "Cambia la cantidad de un ítem del carrito. " +
                      "Si cantidad = 0, elimina el ítem automáticamente."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Carrito actualizado"),
        @ApiResponse(responseCode = "409",
                     description = "Stock insuficiente",
                     content = @Content(schema = @Schema(hidden = true)))
    })
    @PutMapping("/items/{idItem}")
    public ResponseEntity<CarritoResponse> actualizarCantidad(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "ID del ítem en el carrito", example = "1")
            @PathVariable Long idItem,
            @Valid @RequestBody ActualizarCantidadRequest request) {
        var carrito = carritoUseCase.actualizarCantidad(
                userDetails.getUsername(), idItem, request.cantidad());
        return ResponseEntity.ok(CarritoResponse.from(carrito));
    }

    // ── ELIMINAR ÍTEM ────────────────────────────────────────
    @Operation(
        summary = "Eliminar ítem del carrito",
        description = "Elimina un ítem específico del carrito del usuario."
    )
    @ApiResponse(responseCode = "200", description = "Ítem eliminado, carrito actualizado")
    @DeleteMapping("/items/{idItem}")
    public ResponseEntity<CarritoResponse> eliminarItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "ID del ítem a eliminar", example = "1")
            @PathVariable Long idItem) {
        var carrito = carritoUseCase.eliminarItem(
                userDetails.getUsername(), idItem);
        return ResponseEntity.ok(CarritoResponse.from(carrito));
    }

    // ── VACIAR CARRITO ───────────────────────────────────────
    @Operation(
        summary = "Vaciar carrito completo",
        description = "Elimina todos los ítems del carrito del usuario."
    )
    @ApiResponse(responseCode = "204", description = "Carrito vaciado")
    @DeleteMapping
    public ResponseEntity<Void> vaciarCarrito(
            @AuthenticationPrincipal UserDetails userDetails) {
        carritoUseCase.vaciarCarrito(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
