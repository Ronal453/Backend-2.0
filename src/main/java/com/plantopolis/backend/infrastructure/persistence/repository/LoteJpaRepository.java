package com.plantopolis.backend.infrastructure.persistence.repository;

import com.plantopolis.backend.infrastructure.persistence.entity.LoteProduccionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA para lotes de producción botánica.
 */
@Repository
public interface LoteJpaRepository extends JpaRepository<LoteProduccionEntity, Long> {

    /**
     * Filtra lotes por zona, estado y término de búsqueda (código o especie).
     * Incluye CAST explícito a string para compatibilidad con PostgreSQL cuando los parámetros son nulos.
     */
    @Query("""
        SELECT l FROM LoteProduccionEntity l
        WHERE (:idZona IS NULL OR l.idZona = :idZona)
          AND (CAST(:estadoLote AS string) IS NULL OR UPPER(l.estadoLote) = UPPER(CAST(:estadoLote AS string)))
          AND (CAST(:termino AS string) IS NULL OR UPPER(l.codigoLote) LIKE UPPER(CONCAT('%', CAST(:termino AS string), '%')) OR UPPER(l.especie) LIKE UPPER(CONCAT('%', CAST(:termino AS string), '%')))
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

    /** HU35: lotes históricos de un proveedor, más recientes primero. */
    java.util.List<LoteProduccionEntity> findByIdProveedorOrderByFechaSiembraDesc(Long idProveedor);

    /** HU35: todos los lotes con proveedor asignado (base del reporte agregado). */
    java.util.List<LoteProduccionEntity> findByIdProveedorIsNotNull();
}
