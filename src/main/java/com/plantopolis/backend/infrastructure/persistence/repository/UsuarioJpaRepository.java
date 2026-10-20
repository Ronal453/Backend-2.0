package com.plantopolis.backend.infrastructure.persistence.repository;

import com.plantopolis.backend.infrastructure.persistence.entity.UsuarioEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad UsuarioEntity.
 */
public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, Long> {
    Optional<UsuarioEntity> findByCorreo(String correo);
    boolean existsByCorreo(String correo);

    /**
     * Búsqueda paginada de usuarios con filtros opcionales de texto, rol y estado activo.
     * Utiliza CAST(:nombre AS string) para evitar errores de inferencia de tipos nulos en PostgreSQL.
     */
    @Query("""
        SELECT u FROM UsuarioEntity u
        WHERE (CAST(:nombre AS string) IS NULL OR
               LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', CAST(:nombre AS string), '%')) OR
               LOWER(u.correo) LIKE LOWER(CONCAT('%', CAST(:nombre AS string), '%')))
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

    java.util.List<UsuarioEntity> findByIdRolAndActivoTrue(Long idRol);
}
