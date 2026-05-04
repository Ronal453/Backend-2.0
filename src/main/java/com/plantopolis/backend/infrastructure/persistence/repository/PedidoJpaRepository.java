package com.plantopolis.backend.infrastructure.persistence.repository;

import com.plantopolis.backend.infrastructure.persistence.entity.PedidoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para PedidoEntity.
 *
 * Usa JOIN FETCH para cargar en una sola consulta SQL todas las relaciones
 * necesarias: estado del pedido, usuario, detalles con sus productos,
 * y pago con su método y estado.
 *
 * SIN JOIN FETCH: Hibernate haría N+1 queries (una por cada relación lazy),
 * lo cual puede dejar relaciones null si están fuera del contexto de sesión.
 *
 * CON JOIN FETCH: una sola query trae todo → relaciones siempre disponibles
 * → método de pago, nombre del usuario y detalles nunca son null.
 *
 * Ruta destino:
 *   Back/src/main/java/com/plantopolis/backend/
 *   infrastructure/persistence/repository/PedidoJpaRepository.java
 */
public interface PedidoJpaRepository
        extends JpaRepository<PedidoEntity, Long> {

    /**
     * Busca un pedido por ID cargando TODAS sus relaciones en una sola query.
     *
     * JOIN FETCH cargado:
     *   - p.estado    → EstadoPedidoEntity (descripción del estado)
     *   - p.usuario   → UsuarioEntity (email y nombre del cliente)
     *   - p.detalles  → List<DetallePedidoEntity>
     *   - d.producto  → ProductoEntity (nombre e imagen de cada detalle)
     *   - p.pago      → PagoEntity (LEFT JOIN porque puede no existir aún)
     *   - pago.metodo → MetodoPagoEntity (nombre del método de pago)
     *   - pago.estadoPago → EstadoPagoEntity (APROBADO, PENDIENTE, etc.)
     *
     * @param idPedido ID del pedido a buscar
     * @return Optional con el pedido y todas sus relaciones cargadas
     */
    @Query("""
        SELECT DISTINCT p FROM PedidoEntity p
        JOIN FETCH p.estado
        JOIN FETCH p.usuario
        LEFT JOIN FETCH p.detalles d
        LEFT JOIN FETCH d.producto
        LEFT JOIN FETCH p.pago pago
        LEFT JOIN FETCH pago.metodo
        LEFT JOIN FETCH pago.estadoPago
        WHERE p.idPedido = :idPedido
    """)
    Optional<PedidoEntity> findByIdWithRelations(
            @Param("idPedido") Long idPedido);

    /**
     * Busca todos los pedidos de un usuario con todas sus relaciones cargadas.
     * Ordenados del más reciente al más antiguo.
     *
     * @param idUsuario ID del usuario propietario de los pedidos
     * @return lista de pedidos completos ordenados por fecha descendente
     */
    @Query("""
        SELECT DISTINCT p FROM PedidoEntity p
        JOIN FETCH p.estado
        JOIN FETCH p.usuario
        LEFT JOIN FETCH p.detalles d
        LEFT JOIN FETCH d.producto
        LEFT JOIN FETCH p.pago pago
        LEFT JOIN FETCH pago.metodo
        LEFT JOIN FETCH pago.estadoPago
        WHERE p.idUsuario = :idUsuario
        ORDER BY p.fechaPedido DESC
    """)
    List<PedidoEntity> findByIdUsuarioWithRelations(
            @Param("idUsuario") Long idUsuario);

    /**
     * Verifica si ya existe un pedido con el número dado.
     * Usado para garantizar que el número de pedido sea único.
     *
     * @param numeroPedido número de pedido a verificar
     * @return true si ya existe un pedido con ese número
     */
    boolean existsByNumeroPedido(String numeroPedido);

    /**
     * Busca pedidos por usuario sin JOIN FETCH (versión simple).
     * Mantenido por compatibilidad, se prefiere findByIdUsuarioWithRelations.
     */
    List<PedidoEntity> findByIdUsuarioOrderByFechaPedidoDesc(Long idUsuario);
}