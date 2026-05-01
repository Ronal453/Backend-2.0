package com.plantopolis.backend.infrastructure.persistence.repository;

import com.plantopolis.backend.infrastructure.persistence.entity.DetallePedidoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio JPA para DetallePedidoEntity.
 *
 * Se usa para insertar los detalles del pedido DIRECTAMENTE,
 * evitando el problema de Hibernate con @OneToMany @JoinColumn
 * unidireccional que hace:
 *   1. INSERT detalle con ID_PEDIDO = null  ← ORA-01400 aquí
 *   2. UPDATE detalle SET ID_PEDIDO = ?
 *
 * Al guardar los detalles aquí (con idPedido ya asignado),
 * Hibernate hace solo un INSERT con todos los campos correctos.
 *
 * Ruta destino:
 *   Back/src/main/java/com/plantopolis/backend/
 *   infrastructure/persistence/repository/DetallePedidoJpaRepository.java
 */
public interface DetallePedidoJpaRepository
        extends JpaRepository<DetallePedidoEntity, Long> {

    /**
     * Busca todos los detalles de un pedido específico.
     *
     * @param idPedido ID del pedido padre
     * @return lista de detalles con datos del producto enriquecidos (EAGER)
     */
    List<DetallePedidoEntity> findByIdPedido(Long idPedido);
}