package com.plantopolis.backend.infrastructure.adapter.out.persistence;

import com.plantopolis.backend.domain.model.Carrito;
import com.plantopolis.backend.domain.model.ItemCarrito;
import com.plantopolis.backend.domain.port.out.CarritoRepositoryPort;
import com.plantopolis.backend.infrastructure.persistence.entity.CarritoEntity;
import com.plantopolis.backend.infrastructure.persistence.entity.CarritoItemEntity;
import com.plantopolis.backend.infrastructure.persistence.mapper.CarritoMapper;
import com.plantopolis.backend.infrastructure.persistence.repository.CarritoItemJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.repository.CarritoJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

/**
 * Adaptador JPA para el repositorio del carrito de compras.
 *
 * FIX CRÍTICO en cambiarEstado():
 *   Antes usaba carritoRepo.save() que no garantiza flush inmediato.
 *   Hibernate podía no persistir el cambio antes del commit, dejando
 *   el carrito en estado ACTIVO aunque el checkout ya se completó.
 *   SOLUCIÓN: usar saveAndFlush() para escritura inmediata en BD.
 *
 * Ruta destino:
 *   Back/src/main/java/com/plantopolis/backend/
 *   infrastructure/adapter/out/persistence/CarritoJpaAdapter.java
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CarritoJpaAdapter implements CarritoRepositoryPort {

    private final CarritoJpaRepository     carritoRepo;
    private final CarritoItemJpaRepository itemRepo;
    private final CarritoMapper            mapper;

    // EntityManager para limpiar caché de Hibernate cuando es necesario
    @PersistenceContext
    private EntityManager entityManager;

    // ID del estado ACTIVO en tabla ESTADOCARRITO
    private static final Long ESTADO_ACTIVO = 1L;

    /**
     * Busca el carrito activo del usuario.
     * Devuelve Optional vacío si no existe carrito activo.
     */
    @Override
    public Optional<Carrito> buscarCarritoActivo(Long idUsuario) {
        return carritoRepo.findCarritoActivo(idUsuario)
                .map(mapper::toDomain);
    }

    /**
     * Crea un carrito nuevo en estado ACTIVO para el usuario.
     */
    @Override
    public Carrito crearCarrito(Long idUsuario) {
        var entity = CarritoEntity.builder()
                .idUsuario(idUsuario)
                .idEstadoCarrito(ESTADO_ACTIVO)
                .fechaCreacion(LocalDateTime.now())
                .items(new ArrayList<>())
                .build();
        return mapper.toDomain(carritoRepo.save(entity));
    }

    /**
     * Busca un ítem del carrito por idCarrito + idProducto.
     * Usado para detectar si el producto ya está en el carrito.
     */
    @Override
    public Optional<ItemCarrito> buscarItem(Long idCarrito, Long idProducto) {
        return itemRepo.findByIdCarritoAndIdProducto(idCarrito, idProducto)
                .map(mapper::itemToDomain);
    }

    /**
     * Busca un ítem directamente por su ID.
     */
    @Override
    public Optional<ItemCarrito> buscarItemPorId(Long idItem) {
        return itemRepo.findById(idItem)
                .map(mapper::itemToDomain);
    }

    /**
     * Persiste un ítem (insert o update).
     *
     * saveAndFlush + refresh garantizan que Hibernate lea los datos
     * frescos desde BD (incluyendo la relación @ManyToOne producto)
     * en vez de devolver la entidad cacheada sin precio ni nombre.
     */
    @Override
    @Transactional
    public ItemCarrito guardarItem(ItemCarrito item) {
        var entity = CarritoItemEntity.builder()
                .idItem(item.getIdItem())
                .idCarrito(item.getIdCarrito())
                .idProducto(item.getIdProducto())
                .cantidad(item.getCantidad())
                .build();

        // flush: escribe en BD inmediatamente
        var saved = itemRepo.saveAndFlush(entity);

        // refresh: descarta el caché y recarga desde BD con relaciones EAGER
        entityManager.refresh(saved);

        return mapper.itemToDomain(saved);
    }

    /**
     * Elimina un ítem por su ID usando JPQL directo.
     * Evita conflictos con orphanRemoval del CarritoEntity padre.
     */
    @Override
    @Transactional
    public void eliminarItem(Long idItem) {
        itemRepo.deleteByIdItem(idItem);
    }

    /**
     * Vacía todos los ítems de un carrito.
     */
    @Override
    @Transactional
    public void vaciarCarrito(Long idCarrito) {
        itemRepo.deleteByIdCarrito(idCarrito);
    }

    /**
     * Busca un carrito completo por su ID.
     */
    @Override
    public Optional<Carrito> buscarPorId(Long idCarrito) {
        return carritoRepo.findById(idCarrito)
                .map(mapper::toDomain);
    }

    /**
     * Cambia el estado del carrito (ej: ACTIVO → CONVERTIDO al hacer checkout).
     *
     * FIX CRÍTICO: Antes usaba save() que no garantizaba flush inmediato.
     * Hibernate podía hacer lazy-write después del commit, dejando el carrito
     * en estado ACTIVO en BD aunque el checkout ya se completó.
     *
     * SOLUCIÓN: saveAndFlush() fuerza el UPDATE inmediato en BD dentro
     * de la misma transacción, garantizando que el carrito quede CONVERTIDO
     * antes de que el cliente reciba la respuesta del checkout.
     *
     * @param idCarrito ID del carrito a actualizar
     * @param idEstado  nuevo estado (1=ACTIVO, 2=CONVERTIDO, 3=ABANDONADO)
     */
    @Override
    @Transactional
    public void cambiarEstado(Long idCarrito, Long idEstado) {
        carritoRepo.findById(idCarrito).ifPresent(c -> {
            c.setIdEstadoCarrito(idEstado);

            // saveAndFlush: UPDATE inmediato en BD, no espera al final del commit
            // Esto garantiza que el carrito quede CONVERTIDO antes de que
            // la respuesta del checkout llegue al frontend
            carritoRepo.saveAndFlush(c);

            log.debug("Carrito {} → estado {} (flush inmediato)", idCarrito, idEstado);
        });
    }
}