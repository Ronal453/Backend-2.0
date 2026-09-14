package com.plantopolis.backend.infrastructure.persistence.repository;

import com.plantopolis.backend.infrastructure.persistence.entity.CausaMermaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CausaMermaJpaRepository extends JpaRepository<CausaMermaEntity, Long> {
    List<CausaMermaEntity> findByActivoTrueOrderByNombreCausaAsc();
}
