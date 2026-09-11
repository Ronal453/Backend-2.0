package com.plantopolis.backend.infrastructure.persistence.repository;

import com.plantopolis.backend.infrastructure.persistence.entity.PedidoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PedidoJpaRepository extends JpaRepository<PedidoEntity, Long> {

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
    Optional<PedidoEntity> findByIdWithRelations(@Param("idPedido") Long idPedido);

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
    List<PedidoEntity> findByIdUsuarioWithRelations(@Param("idUsuario") Long idUsuario);

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
    Page<PedidoEntity> findAllAdminWithFilters(@Param("estado") String estado, Pageable pageable);

    @Query("""
        SELECT e.descripcionEstado, COUNT(p)
        FROM PedidoEntity p JOIN p.estado e
        GROUP BY e.descripcionEstado
    """)
    List<Object[]> countPorEstado();

    boolean existsByNumeroPedido(String numeroPedido);

    List<PedidoEntity> findByIdUsuarioOrderByFechaPedidoDesc(Long idUsuario);

    /** Pedidos por rango de fecha, para exportación CSV (LEFT JOIN FETCH detalles evita LazyInit). */
    @Query("""
        SELECT DISTINCT p FROM PedidoEntity p
        JOIN FETCH p.estado
        JOIN FETCH p.usuario
        LEFT JOIN FETCH p.detalles d
        WHERE (:fechaInicio IS NULL OR p.fechaPedido >= :fechaInicio)
          AND (:fechaFin IS NULL OR p.fechaPedido <= :fechaFin)
        ORDER BY p.fechaPedido DESC
    """)
    List<PedidoEntity> buscarPorRangoFecha(
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin
    );
}