package com.plantopolis.backend.infrastructure.adapter.in.web;

import com.plantopolis.backend.domain.port.in.GestionarCarritoUseCase;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.ActualizarCantidadRequest;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.AgregarItemRequest;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.CarritoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carrito")
@RequiredArgsConstructor
public class CarritoController {

    private final GestionarCarritoUseCase carritoUseCase;

    /** GET /api/carrito — Ver mi carrito */
    @GetMapping
    public ResponseEntity<CarritoResponse> verCarrito(
            @AuthenticationPrincipal UserDetails userDetails) {
        var carrito = carritoUseCase.verCarrito(userDetails.getUsername());
        return ResponseEntity.ok(CarritoResponse.from(carrito));
    }

    /** POST /api/carrito/items — Agregar producto */
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

    /** PUT /api/carrito/items/{idItem} — Actualizar cantidad */
    @PutMapping("/items/{idItem}")
    public ResponseEntity<CarritoResponse> actualizarCantidad(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long idItem,
            @Valid @RequestBody ActualizarCantidadRequest request) {
        var carrito = carritoUseCase.actualizarCantidad(
                userDetails.getUsername(),
                idItem,
                request.cantidad());
        return ResponseEntity.ok(CarritoResponse.from(carrito));
    }

    /** DELETE /api/carrito/items/{idItem} — Eliminar item */
    @DeleteMapping("/items/{idItem}")
    public ResponseEntity<CarritoResponse> eliminarItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long idItem) {
        var carrito = carritoUseCase.eliminarItem(
                userDetails.getUsername(), idItem);
        return ResponseEntity.ok(CarritoResponse.from(carrito));
    }

    /** DELETE /api/carrito — Vaciar carrito */
    @DeleteMapping
    public ResponseEntity<Void> vaciarCarrito(
            @AuthenticationPrincipal UserDetails userDetails) {
        carritoUseCase.vaciarCarrito(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
