package com.plantopolis.backend.infrastructure.adapter.in.web;

import com.plantopolis.backend.domain.port.in.GestionarPedidosAdminUseCase;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.ActualizarEstadoPedidoRequest;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.PedidoResponse;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para la gestión de pedidos desde el panel admin.
 * 
 * Todos los endpoints requieren rol ADMINISTRADOR.
 *
 * Endpoints:
 *   GET   /api/admin/pedidos           → listar todos (filtro por estado)
 *   PATCH /api/admin/pedidos/{id}/estado → cambiar estado del pedido
 *
 * Al cambiar estado a CANCELADO:
 *   → El stock de cada producto se restaura automáticamente
 *   → El cliente recibe un email de notificación
 *
 */
@RestController
@RequestMapping("/api/admin/pedidos")
@RequiredArgsConstructor
@Tag(
    name = "Admin — Pedidos",
    description = "Gestión de pedidos desde el panel de administración. " +
                  "🔒 Requiere token JWT + rol ADMINISTRADOR."
)
@SecurityRequirement(name = "Bearer Authentication")
public class AdminPedidoController {

    private final GestionarPedidosAdminUseCase adminPedidoUseCase;

    // ── LISTAR TODOS LOS PEDIDOS ──────────────────────────────────────────
    @Operation(
        summary = "Listar todos los pedidos",
        description = "Devuelve todos los pedidos del sistema. " +
                      "Se puede filtrar por estado: PENDIENTE, PREPARANDO, " +
                      "ENVIADO, ENTREGADO, CANCELADO. " +
                      "Si no se especifica estado, devuelve todos."
    )
    @ApiResponse(responseCode = "200", description = "Lista de pedidos paginada")
    @ApiResponse(responseCode = "403", description = "No es administrador")
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Page<PedidoResponse>> listar(
            @Parameter(
                description = "Filtrar por estado (PENDIENTE/PREPARANDO/ENVIADO/ENTREGADO/CANCELADO)",
                example = "PENDIENTE"
            )
            @RequestParam(required = false) String estado,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        // Ordenar siempre del más reciente al más antiguo
        var pageable = PageRequest.of(page, size,
                Sort.by("fechaPedido").descending());

        // listarTodos() → findAllAdminWithFilters() en JPA
        return ResponseEntity.ok(
                adminPedidoUseCase
                        .listarTodos(estado, pageable)
                        .map(PedidoResponse::from)
        );
    }

    // ── ACTUALIZAR ESTADO ────────────────────────────────────────────────
    @Operation(
        summary = "Actualizar estado de un pedido",
        description = """
            Cambia el estado de un pedido. Las transiciones válidas son:
            - PENDIENTE  → PREPARANDO, CANCELADO
            - PREPARANDO → ENVIADO, CANCELADO
            - ENVIADO    → ENTREGADO, CANCELADO
            - ENTREGADO  → (estado final)
            - CANCELADO  → (estado final)
            
            **Al cancelar:** se restaura automáticamente el stock de todos
            los productos del pedido y se notifica al cliente por email.
            
            **Al cambiar a cualquier estado:** se envía email al cliente.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estado actualizado correctamente"),
        @ApiResponse(responseCode = "409",
                     description = "Pedido no encontrado o transición no permitida"),
        @ApiResponse(responseCode = "400", description = "Estado inválido"),
        @ApiResponse(responseCode = "403", description = "No es administrador")
    })
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<PedidoResponse> actualizarEstado(
            @Parameter(description = "ID del pedido", example = "1")
            @PathVariable Long id,

            @Valid @RequestBody ActualizarEstadoPedidoRequest request
    ) {
        // Llama al servicio que valida la transición, restaura stock si es
        // necesario, persiste el cambio y envía el email al cliente
        var pedidoActualizado = adminPedidoUseCase
                .actualizarEstado(id, request.nuevoEstado());

        return ResponseEntity.ok(PedidoResponse.from(pedidoActualizado));
    }
}