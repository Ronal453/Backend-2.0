package com.plantopolis.backend.infrastructure.persistence.repository;

import com.plantopolis.backend.infrastructure.persistence.entity.ComentarioTareaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComentarioTareaJpaRepository extends JpaRepository<ComentarioTareaEntity, Long> {
    List<ComentarioTareaEntity> findByIdTareaOrderByFechaCreacionAsc(Long idTarea);
}
