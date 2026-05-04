package com.plantopolis.backend.infrastructure.persistence.repository;

import com.plantopolis.backend.infrastructure.persistence.entity.CarritoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

/**
 * Repositorio JPA para CarritoEntity.
 *
 * FIX en findCarritoActivo:
 *   La query original buscaba el carrito activo del usuario.
 *   Después del checkout, el carrito queda en estado CONVERTIDO (id=2).
 *   Si Hibernate devuelve el carrito cacheado antes del flush,
 *   podría seguir apareciendo como ACTIVO en la misma sesión.
 *
 *   La query ya filtra por idEstadoCarrito = 1 (ACTIVO), por lo que
 *   cuando el carrito pase a CONVERTIDO (2) esta query devolverá
 *   Optional.empty() → CarritoService crea uno nuevo vacío automáticamente.
 *
 * Ruta destino:
 *   Back/src/main/java/com/plantopolis/backend/
 *   infrastructure/persistence/repository/CarritoJpaRepository.java
 */
public interface CarritoJpaRepository
        extends JpaRepository<CarritoEntity, Long> {

    /**
     * Busca el carrito ACTIVO (idEstadoCarrito = 1) del usuario.
     *
     * Si el carrito fue marcado como CONVERTIDO (2) en el checkout,
     * esta query devuelve Optional.empty(), lo que hace que
     * CarritoService.verCarrito() cree un carrito nuevo vacío.
     *
     * ORDER BY fecha DESC garantiza que si hay múltiples carritos activos
     * (no debería, pero por seguridad), se usa el más reciente.
     *
     * @param idUsuario ID del usuario propietario del carrito
     * @return Optional con el carrito activo, o vacío si no existe
     */
    @Query("""
        SELECT c FROM CarritoEntity c
        WHERE c.idUsuario = :idUsuario
          AND c.idEstadoCarrito = 1
        ORDER BY c.fechaCreacion DESC
    """)
    Optional<CarritoEntity> findCarritoActivo(
            @Param("idUsuario") Long idUsuario);
}