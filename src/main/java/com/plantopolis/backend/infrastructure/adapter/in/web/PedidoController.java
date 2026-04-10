package com.plantopolis.backend.infrastructure.adapter.in.web;

import com.plantopolis.backend.domain.port.in.ProcesarPedidoUseCase;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final ProcesarPedidoUseCase pedidoUseCase;

    /** POST /api/pedidos/checkout */
    @PostMapping("/checkout")
    public ResponseEntity<PedidoResponse> checkout(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody CheckoutRequest request) {
        var pedido = pedidoUseCase.procesarPedido(
                user.getUsername(),
                request.idMetodoPago(),
                request.direccionEnvio());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(PedidoResponse.from(pedido));
    }

    /** GET /api/pedidos — Historial del cliente */
    @GetMapping
    public ResponseEntity<List<PedidoResponse>> historial(
            @AuthenticationPrincipal UserDetails user) {
        var lista = pedidoUseCase.obtenerHistorial(user.getUsername())
                .stream().map(PedidoResponse::from).toList();
        return ResponseEntity.ok(lista);
    }

    /** GET /api/pedidos/{id} — Detalle */
    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> detalle(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {
        var pedido = pedidoUseCase.obtenerDetalle(user.getUsername(), id);
        return ResponseEntity.ok(PedidoResponse.from(pedido));
    }
}