package com.plantopolis.backend.infrastructure.persistence.repository;

import com.plantopolis.backend.infrastructure.persistence.entity.CarritoItemEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface CarritoItemJpaRepository
        extends JpaRepository<CarritoItemEntity, Long> {

    Optional<CarritoItemEntity> findByIdCarritoAndIdProducto(
            Long idCarrito, Long idProducto);

    @Modifying
    @Transactional
    @Query("DELETE FROM CarritoItemEntity ci WHERE ci.idCarrito = :idCarrito")
    void deleteByIdCarrito(@Param("idCarrito") Long idCarrito);

    // ← NUEVO: elimina directamente por ID del ítem con JPQL para evitar
    //   conflictos con orphanRemoval del CarritoEntity padre
    @Modifying
    @Transactional
    @Query("DELETE FROM CarritoItemEntity ci WHERE ci.idItem = :idItem")
    void deleteByIdItem(@Param("idItem") Long idItem);
}