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
          AND (:termino IS NULL OR UPPER(l.codigoLote) LIKE UPPER(CONCAT('%', :termino, '%')) OR UPPER(l.especie) LIKE UPPER(CONCAT('%', :termino, '%')))
    """)
    Page<LoteProduccionEntity> buscarConFiltros(
            @Param("idZona") Long idZona,
            @Param("estadoLote") String estadoLote,
            @Param("termino") String termino,
            Pageable pageable
    );
    
    long countByIdZonaAndEstadoLoteNotIn(Long idZona, java.util.Collection<String> estados);
    
    java.util.List<LoteProduccionEntity> findByIdZonaAndEstadoLoteNotInOrderByFechaSiembraDesc(Long idZona, java.util.Collection<String> estados);

    @Query("""
        SELECT COALESCE(SUM(l.cantidadActual), 0)
        FROM LoteProduccionEntity l
        WHERE l.idZona = :idZona
          AND UPPER(l.estadoLote) NOT IN ('DESCARTADO', 'EN_TIENDA')
    """)
    long sumarPlantasActivasPorZona(@Param("idZona") Long idZona);
}
