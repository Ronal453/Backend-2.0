package com.plantopolis.backend.infrastructure.persistence.repository;

import com.plantopolis.backend.infrastructure.persistence.entity.HistorialEstadoLoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialEstadoLoteJpaRepository extends JpaRepository<HistorialEstadoLoteEntity, Long> {
    List<HistorialEstadoLoteEntity> findByIdLoteOrderByFechaCambioDesc(Long idLote);
}
