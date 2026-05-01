package com.plantopolis.backend.infrastructure.persistence.repository;

import com.plantopolis.backend.infrastructure.persistence.entity.PagoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad PagoEntity.
 *
 * Necesario para guardar el Pago directamente en la BD
 * sin depender del cascade de PedidoEntity, ya que el
 * @OneToOne tiene insertable=false / updatable=false.
 *
 * Ruta destino:
 *   Back/src/main/java/com/plantopolis/backend/
 *   infrastructure/persistence/repository/PagoJpaRepository.java
 */
public interface PagoJpaRepository extends JpaRepository<PagoEntity, Long> {

    /**
     * Busca el pago asociado a un pedido específico.
     * Útil para consultas de detalle de pedido o auditoría.
     *
     * @param idPedido ID del pedido
     * @return Optional con el pago si existe
     */
    Optional<PagoEntity> findByIdPedido(Long idPedido);
}