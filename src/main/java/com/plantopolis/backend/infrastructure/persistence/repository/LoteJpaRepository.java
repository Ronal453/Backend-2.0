package com.plantopolis.backend.infrastructure.persistence.repository;

import com.plantopolis.backend.infrastructure.persistence.entity.LoteProduccionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LoteJpaRepository extends JpaRepository<LoteProduccionEntity, Long> {

    @Query("""
        SELECT l FROM LoteProduccionEntity l
        WHERE (:idZona IS NULL OR l.idZona = :idZona)
          AND (:estadoLote IS NULL OR UPPER(l.estadoLote) = UPPER(:estadoLote))
        ORDER BY l.fechaSiembra DESC, l.idLote DESC
    """)
    Page<LoteProduccionEntity> buscarConFiltros(
            @Param("idZona") Long idZona,
            @Param("estadoLote") String estadoLote,
            Pageable pageable
    );
    
    long countByIdZonaAndEstadoLoteNot(Long idZona, String estadoLote);
    
    java.util.List<LoteProduccionEntity> findByIdZonaAndEstadoLoteNotOrderByFechaSiembraDesc(Long idZona, String estadoLote);

    @Query("""
        SELECT COALESCE(SUM(l.cantidadActual), 0)
        FROM LoteProduccionEntity l
        WHERE l.idZona = :idZona
          AND UPPER(l.estadoLote) <> 'DESCARTADO'
    """)
    long sumarPlantasActivasPorZona(@Param("idZona") Long idZona);
}
