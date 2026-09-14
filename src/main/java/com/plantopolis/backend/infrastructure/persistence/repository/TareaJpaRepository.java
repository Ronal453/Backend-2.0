package com.plantopolis.backend.infrastructure.persistence.repository;

import com.plantopolis.backend.infrastructure.persistence.entity.TareaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TareaJpaRepository extends JpaRepository<TareaEntity, Long> {

    @Query("""
        SELECT t FROM TareaEntity t
        WHERE (:idTrabajador IS NULL OR t.idTrabajadorAsignado = :idTrabajador)
          AND (:estado IS NULL OR UPPER(t.estadoTarea) = UPPER(:estado))
          AND (:prioridad IS NULL OR UPPER(t.prioridad) = UPPER(:prioridad))
        ORDER BY t.fechaLimite ASC, t.idTarea DESC
    """)
    List<TareaEntity> buscarConFiltros(
            @Param("idTrabajador") Long idTrabajador,
            @Param("estado") String estado,
            @Param("prioridad") String prioridad
    );
}
