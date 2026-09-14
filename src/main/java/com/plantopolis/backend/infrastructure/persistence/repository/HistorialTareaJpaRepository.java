package com.plantopolis.backend.infrastructure.persistence.repository;

import com.plantopolis.backend.infrastructure.persistence.entity.HistorialTareaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialTareaJpaRepository extends JpaRepository<HistorialTareaEntity, Long> {
    List<HistorialTareaEntity> findByIdTareaOrderByFechaCambioAsc(Long idTarea);
}
