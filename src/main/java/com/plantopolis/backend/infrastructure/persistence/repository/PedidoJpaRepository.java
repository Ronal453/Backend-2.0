package com.plantopolis.backend.infrastructure.persistence.repository;

import com.plantopolis.backend.infrastructure.persistence.entity.PedidoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para PedidoEntity.
 *
 *   - findAllAdminWithFilters(): lista todos los pedidos con filtro de estado
 *   - countTotal(): conteo total para dashboard
 *   - countPorEstado(): distribución por estado para el gráfico
 *
 */
public interface PedidoJpaRepository extends JpaRepository<PedidoEntity, Long> {

    /** Busca un pedido por ID con todas sus relaciones cargadas. */
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

    /** Busca todos los pedidos de un usuario con relaciones. */
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
     * Lista todos los pedidos del sistema con filtro opcional por estado.
     * Incluye JOIN FETCH de relaciones para evitar N+1 queries.
     *
     * countQuery separado: necesario cuando se usa JOIN FETCH + Pageable
     * en Spring Data JPA para que la paginación funcione correctamente.
     *
     * @param estado descripción del estado (ej: "PENDIENTE") o null para todos
     */
    @Query(
        value = """
            SELECT DISTINCT p FROM PedidoEntity p
            JOIN FETCH p.estado
            JOIN FETCH p.usuario
            LEFT JOIN FETCH p.detalles d
            LEFT JOIN FETCH d.producto
            LEFT JOIN FETCH p.pago pago
            LEFT JOIN FETCH pago.metodo
            LEFT JOIN FETCH pago.estadoPago
            WHERE (:estado IS NULL OR p.estado.descripcionEstado = :estado)
            ORDER BY p.fechaPedido DESC
        """,
        countQuery = """
            SELECT COUNT(DISTINCT p) FROM PedidoEntity p
            JOIN p.estado
            WHERE (:estado IS NULL OR p.estado.descripcionEstado = :estado)
        """
    )
    Page<PedidoEntity> findAllAdminWithFilters(
            @Param("estado") String estado,
            Pageable pageable
    );

    
    /**
     * Distribución de pedidos por estado para el gráfico del dashboard.
     * Devuelve List<Object[]> donde cada elemento es [descripcionEstado, count].
     */
    @Query("""
        SELECT e.descripcionEstado, COUNT(p)
        FROM PedidoEntity p
        JOIN p.estado e
        GROUP BY e.descripcionEstado
    """)
    List<Object[]> countPorEstado();

    /** Verifica si ya existe un pedido con el número dado (para generación única). */
    boolean existsByNumeroPedido(String numeroPedido);

    /** Busca pedidos por usuario sin JOIN FETCH (versión simple). */
    List<PedidoEntity> findByIdUsuarioOrderByFechaPedidoDesc(Long idUsuario);
}