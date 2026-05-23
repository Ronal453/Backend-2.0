package com.plantopolis.backend.infrastructure.persistence.repository;

import com.plantopolis.backend.infrastructure.persistence.entity.EstadoPedidoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 *
 * Necesario para el AdminPedidoService:
 * cuando el admin envía el nombre del nuevo estado ("ENVIADO"),
 * necesitamos buscar su ID en la tabla ESTADOPEDIDO para hacer el UPDATE.
 *
 * Los estados en la BD son:
 *   1=PENDIENTE, 2=PREPARANDO, 3=ENVIADO, 4=ENTREGADO, 5=CANCELADO
 *
 */
public interface EstadoPedidoJpaRepository
        extends JpaRepository<EstadoPedidoEntity, Long> {

    /**
     * Busca un estado de pedido por su descripción.
     * Ejemplo: findByDescripcionEstado("ENVIADO") → Optional<EstadoPedidoEntity(id=3)>
     *
     * @param descripcionEstado descripción en mayúsculas (como está en la BD)
     * @return Optional con el estado encontrado
     */
    Optional<EstadoPedidoEntity> findByDescripcionEstado(String descripcionEstado);
}