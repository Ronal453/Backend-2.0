package com.plantopolis.backend.infrastructure.persistence.repository;

import com.plantopolis.backend.infrastructure.persistence.entity.MetodoPagoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MetodoPagoJpaRepository
        extends JpaRepository<MetodoPagoEntity, Long> {
}