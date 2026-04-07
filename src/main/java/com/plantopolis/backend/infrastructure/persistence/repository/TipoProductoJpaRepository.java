package com.plantopolis.backend.infrastructure.persistence.repository;

import com.plantopolis.backend.infrastructure.persistence.entity.TipoProductoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoProductoJpaRepository extends JpaRepository<TipoProductoEntity, Long> {
}
