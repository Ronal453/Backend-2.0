package com.plantopolis.backend.infrastructure.persistence.repository;

import com.plantopolis.backend.infrastructure.persistence.entity.UsuarioEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, Long> {
    Optional<UsuarioEntity> findByCorreo(String correo);
    boolean existsByCorreo(String correo);

    @Query("""
        SELECT u FROM UsuarioEntity u
        WHERE (:nombre IS NULL OR
               LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', :nombre, '%')) OR
               LOWER(u.correo) LIKE LOWER(CONCAT('%', :nombre, '%')))
          AND (:idRol IS NULL OR u.idRol = :idRol)
          AND (:activo IS NULL OR u.activo = :activo)
        ORDER BY u.nombreCompleto
    """)
    Page<UsuarioEntity> buscarConFiltros(
            @Param("nombre") String nombre,
            @Param("idRol") Long idRol,
            @Param("activo") Boolean activo,
            Pageable pageable
    );
}