package com.plantopolis.backend.infrastructure.persistence.repository;

import com.plantopolis.backend.infrastructure.persistence.entity.CarritoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface CarritoJpaRepository extends JpaRepository<CarritoEntity, Long> {

    // Buscar carrito ACTIVO (id_estado_carrito = 1) del usuario
    @Query("""
        SELECT c FROM CarritoEntity c
        WHERE c.idUsuario = :idUsuario
          AND c.idEstadoCarrito = 1
        ORDER BY c.fechaCreacion DESC
    """)
    Optional<CarritoEntity> findCarritoActivo(@Param("idUsuario") Long idUsuario);
}
