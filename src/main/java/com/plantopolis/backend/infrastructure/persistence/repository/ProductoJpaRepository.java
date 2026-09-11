package com.plantopolis.backend.infrastructure.persistence.repository;

import com.plantopolis.backend.infrastructure.persistence.entity.ProductoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ProductoJpaRepository extends JpaRepository<ProductoEntity, Long> {

    @Query("""
        SELECT p FROM ProductoEntity p
        WHERE p.activo = true
          AND (:nombre IS NULL OR 
               LOWER(p.nombreProducto) LIKE LOWER(CONCAT('%', :nombre, '%')))
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

    @Query("""
        SELECT p FROM ProductoEntity p
        WHERE (:nombre IS NULL OR 
               LOWER(p.nombreProducto) LIKE LOWER(CONCAT('%', :nombre, '%')))
          AND (:idCategoria IS NULL OR p.idCategoria = :idCategoria)
          AND (:idTipo IS NULL OR p.idTipo = :idTipo)
    """)
    Page<ProductoEntity> buscarTodosAdmin(
            @Param("nombre") String nombre,
            @Param("idCategoria") Long idCategoria,
            @Param("idTipo") Long idTipo,
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