package com.plantopolis.backend.infrastructure.persistence.repository;

import com.plantopolis.backend.infrastructure.persistence.entity.DetallePedidoEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DetallePedidoJpaRepository
        extends JpaRepository<DetallePedidoEntity, Long> {

    /** Busca todos los detalles de un pedido específico. */
    List<DetallePedidoEntity> findByIdPedido(Long idPedido);

    /**
     * Devuelve los productos más vendidos agrupando por producto
     * y sumando unidades e ingresos.
     *
     * Excluye los pedidos CANCELADOS (idEstado = 5) para que
     * los reportes reflejen ventas reales, no canceladas.
     *
     * Resultado: List<Object[]> donde cada elemento es:
     *   [0] = idProducto (Long)
     *   [1] = nombreProducto (String)
     *   [2] = totalVendido (Long) — suma de cantidades
     *   [3] = totalIngresos (BigDecimal) — suma de precio * cantidad
     *
     * El Pageable permite limitar el resultado a los top N.
     * Uso: findTopProductos(PageRequest.of(0, 5)) → top 5
     */
    @Query("""
        SELECT d.idProducto,
               pr.nombreProducto,
               SUM(d.cantidad),
               SUM(d.precioUnitario * d.cantidad)
        FROM DetallePedidoEntity d
        JOIN d.producto pr
        JOIN d.pedido ped
        WHERE ped.idEstado != 5
        GROUP BY d.idProducto, pr.nombreProducto
        ORDER BY SUM(d.cantidad) DESC
    """)
    List<Object[]> findTopProductos(Pageable pageable);
}