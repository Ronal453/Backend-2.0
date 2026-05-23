package com.plantopolis.backend.domain.port.in;

import com.plantopolis.backend.domain.model.Pedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * El admin puede:
 *   - Ver TODOS los pedidos del sistema (no solo los suyos)
 *   - Cambiar el estado de un pedido (ciclo: PENDIENTE → PREPARANDO → ENVIADO → ENTREGADO)
 *   - Cancelar un pedido (libera el stock y notifica al cliente)
 *
 * Flujo de estados válidos:
 *   PENDIENTE  → PREPARANDO, CANCELADO
 *   PREPARANDO → ENVIADO, CANCELADO
 *   ENVIADO    → ENTREGADO, CANCELADO
 *   ENTREGADO  → (estado final, no se puede cambiar)
 *   CANCELADO  → (estado final, no se puede cambiar)
 *
 */
public interface GestionarPedidosAdminUseCase {

    /**
     * Lista todos los pedidos del sistema con filtro opcional por estado.
     *
     * @param estado   filtro por estado (ej: "PENDIENTE") — null para todos
     * @param pageable paginación y ordenamiento
     * @return página de pedidos con datos completos (cliente, detalles, pago)
     */
    Page<Pedido> listarTodos(String estado, Pageable pageable);

    /**
     * Actualiza el estado de un pedido.
     *
     * Lógica interna:
     *   1. Verifica que el pedido exista
     *   2. Valida que la transición de estado sea válida
     *   3. Si el nuevo estado es CANCELADO, restaura el stock de cada producto
     *   4. Persiste el nuevo estado
     *   5. Envía email de notificación al cliente
     *
     * @param idPedido    ID del pedido a actualizar
     * @param nuevoEstado descripción del nuevo estado (ej: "ENVIADO")
     * @return el pedido actualizado con el nuevo estado
     * @throws RuntimeException si la transición no es válida o el pedido no existe
     */
    Pedido actualizarEstado(Long idPedido, String nuevoEstado);
}