package com.plantopolis.backend.infrastructure.adapter.in.web;

import com.plantopolis.backend.domain.port.in.ProcesarPedidoUseCase;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.CheckoutRequest;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.PedidoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
@Tag(
    name = "Pedidos",
    description = "Proceso de compra y consulta de pedidos del cliente. " +
                  "🔒 Requiere token JWT. " +
                  "El checkout convierte el carrito activo en un pedido, " +
                  "reduce el stock y envía confirmación por email."
)
@SecurityRequirement(name = "Bearer Authentication")
public class PedidoController {

    private final ProcesarPedidoUseCase pedidoUseCase;

    // ── CHECKOUT ─────────────────────────────────────────────
    @Operation(
        summary = "Procesar pedido (checkout)",
        description = """
            Convierte el carrito activo en un pedido confirmado.
            
            **Proceso interno:**
            1. Valida que el carrito no esté vacío
            2. Valida stock de cada producto
            3. Crea el Pedido con estado PENDIENTE
            4. Registra el Pago como APROBADO
            5. Reduce el stock de cada producto
            6. Marca el carrito como CONVERTIDO
            7. Envía email de confirmación al cliente
            
            **Métodos de pago disponibles:**
            - 1 = TARJETA_CREDITO
            - 2 = TARJETA_DEBITO
            - 3 = TRANSFERENCIA
            - 4 = EFECTIVO
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Pedido creado correctamente",
            content = @Content(
                schema = @Schema(implementation = PedidoResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "idPedido": 1,
                      "numeroPedido": "PL-20241215-4823",
                      "estado": "PENDIENTE",
                      "fechaPedido": "2024-12-15T10:30:00",
                      "direccionEnvio": "Calle 123, Bogotá",
                      "total": 85000.00,
                      "detalles": [
                        {
                          "nombreProducto": "Pothos Dorado",
                          "cantidad": 2,
                          "precioUnitario": 25000.00,
                          "subtotal": 50000.00
                        }
                      ],
                      "pago": {
                        "metodoPago": "TARJETA_CREDITO",
                        "estadoPago": "APROBADO",
                        "monto": 85000.00
                      }
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Carrito vacío, stock insuficiente o método de pago inválido"
        ),
        @ApiResponse(responseCode = "401", description = "Token JWT requerido")
    })
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

    // ── HISTORIAL ────────────────────────────────────────────
    @Operation(
        summary = "Ver historial de pedidos",
        description = "Devuelve todos los pedidos del cliente autenticado, " +
                      "ordenados del más reciente al más antiguo."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de pedidos del cliente"),
        @ApiResponse(responseCode = "401", description = "Token JWT requerido")
    })
    @GetMapping
    public ResponseEntity<List<PedidoResponse>> historial(
            @AuthenticationPrincipal UserDetails user) {
        var lista = pedidoUseCase.obtenerHistorial(user.getUsername())
                .stream()
                .map(PedidoResponse::from)
                .toList();
        return ResponseEntity.ok(lista);
    }

    // ── DETALLE ──────────────────────────────────────────────
    @Operation(
        summary = "Ver detalle de un pedido",
        description = "Devuelve el detalle completo de un pedido específico. " +
                      "Solo puedes ver tus propios pedidos."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Detalle del pedido"),
        @ApiResponse(responseCode = "409",
                     description = "Pedido no encontrado o no te pertenece"),
        @ApiResponse(responseCode = "401", description = "Token JWT requerido")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> detalle(
            @AuthenticationPrincipal UserDetails user,
            @Parameter(description = "ID del pedido", example = "1")
            @PathVariable Long id) {
        var pedido = pedidoUseCase.obtenerDetalle(user.getUsername(), id);
        return ResponseEntity.ok(PedidoResponse.from(pedido));
    }
}
