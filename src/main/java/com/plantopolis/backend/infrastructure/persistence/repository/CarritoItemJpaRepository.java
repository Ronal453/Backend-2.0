package com.plantopolis.backend.infrastructure.persistence.repository;

import com.plantopolis.backend.infrastructure.persistence.entity.CarritoItemEntity;
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
    @Query("DELETE FROM CarritoItemEntity ci WHERE ci.idCarrito = :idCarrito")
    void deleteByIdCarrito(@Param("idCarrito") Long idCarrito);
}
