package com.plantopolis.backend.infrastructure.persistence.repository;

import com.plantopolis.backend.infrastructure.persistence.entity.ProveedorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio Spring Data para la tabla PROVEEDOR.
 */
@Repository
public interface ProveedorJpaRepository extends JpaRepository<ProveedorEntity, Long> {

    List<ProveedorEntity> findAllByOrderByNombreAsc();

    List<ProveedorEntity> findByActivoTrueOrderByNombreAsc();

    @Query("SELECT COUNT(p) > 0 FROM ProveedorEntity p WHERE UPPER(p.nombre) = UPPER(:nombre)")
    boolean existsByNombreIgnoreCase(@Param("nombre") String nombre);

    @Query("SELECT COUNT(p) > 0 FROM ProveedorEntity p WHERE UPPER(p.nombre) = UPPER(:nombre) AND p.idProveedor <> :idProveedor")
    boolean existsByNombreIgnoreCaseAndIdProveedorNot(@Param("nombre") String nombre, @Param("idProveedor") Long idProveedor);

    /**
     * Búsqueda con filtros opcionales: cada parámetro nulo se ignora.
     * El término se compara contra nombre y contacto sin distinguir mayúsculas.
     * Se usa CAST explícito a string para evitar errores de inferencia en PostgreSQL.
     */
    @Query("""
        SELECT p FROM ProveedorEntity p
        WHERE (CAST(:tipoInsumo AS string) IS NULL OR UPPER(p.tipoInsumo) = UPPER(CAST(:tipoInsumo AS string)))
          AND (:activo IS NULL OR p.activo = :activo)
          AND (CAST(:termino AS string) IS NULL
               OR UPPER(p.nombre) LIKE UPPER(CONCAT('%', CAST(:termino AS string), '%'))
               OR UPPER(p.contacto) LIKE UPPER(CONCAT('%', CAST(:termino AS string), '%')))
        ORDER BY p.nombre ASC
    """)
    List<ProveedorEntity> buscarConFiltros(
            @Param("termino") String termino,
            @Param("tipoInsumo") String tipoInsumo,
            @Param("activo") Boolean activo
    );
}
