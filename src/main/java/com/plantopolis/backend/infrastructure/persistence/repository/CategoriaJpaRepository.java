package com.plantopolis.backend.infrastructure.persistence.repository;

import com.plantopolis.backend.infrastructure.persistence.entity.CategoriaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CategoriaJpaRepository extends JpaRepository<CategoriaEntity, Long> {

    // Devuelve categorías con nombre único (evita duplicados en BD)
    @Query("SELECT c FROM CategoriaEntity c WHERE c.idCategoria IN " +
           "(SELECT MIN(c2.idCategoria) FROM CategoriaEntity c2 GROUP BY c2.nombreCategoria) " +
           "ORDER BY c.nombreCategoria")
    List<CategoriaEntity> findDistinctByNombreCategoria();
}