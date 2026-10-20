package com.plantopolis.backend.infrastructure.persistence.repository;

import com.plantopolis.backend.infrastructure.persistence.entity.ProductoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repositorio JPA para consultas de productos del catálogo público y panel administrativo.
 */
public interface ProductoJpaRepository extends JpaRepository<ProductoEntity, Long> {

    /**
     * Filtra productos activos del catálogo público.
     * Utiliza CAST(:nombre AS string) para garantizar compatibilidad de tipos en PostgreSQL cuando el filtro es nulo.
     */
    @Query("""
        SELECT p FROM ProductoEntity p
        WHERE p.activo = true
          AND (CAST(:nombre AS string) IS NULL OR 
               LOWER(p.nombreProducto) LIKE LOWER(CONCAT('%', CAST(:nombre AS string), '%')))
          AND (:idCategoria IS NULL OR p.idCategoria = :idCategoria)
          AND (:idTipo IS NULL OR p.idTipo = :idTipo)
          AND (:precioMin IS NULL OR p.precio >= :precioMin)
          AND (:precioMax IS NULL OR p.precio <= :precioMax)
    """)
    Page<ProductoEntity> buscarConFiltros(
            @Param("nombre") String nombre,
            @Param("idCategoria") Long idCategoria,
            @Param("idTipo") Long idTipo,
            @Param("precioMin") BigDecimal precioMin,
            @Param("precioMax") BigDecimal precioMax,
            Pageable pageable
    );

    /**
     * Filtra productos para la vista de administración incluyendo activos e inactivos.
     */
    @Query("""
        SELECT p FROM ProductoEntity p
        WHERE (CAST(:nombre AS string) IS NULL OR 
               LOWER(p.nombreProducto) LIKE LOWER(CONCAT('%', CAST(:nombre AS string), '%')))
          AND (:idCategoria IS NULL OR p.idCategoria = :idCategoria)
          AND (:idTipo IS NULL OR p.idTipo = :idTipo)
          AND (:activo IS NULL OR p.activo = :activo)
    """)
    Page<ProductoEntity> buscarTodosAdmin(
            @Param("nombre") String nombre,
            @Param("idCategoria") Long idCategoria,
            @Param("idTipo") Long idTipo,
            @Param("activo") Boolean activo,
            Pageable pageable
    );

    Long countByActivoTrue();

    @Query("""
        SELECT p FROM ProductoEntity p
        WHERE p.activo = true AND p.stock <= p.stockMinimoAlerta
        ORDER BY p.stock ASC
    """)
    List<ProductoEntity> buscarConStockCritico();
}
