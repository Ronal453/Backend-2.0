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
import java.time.ZoneId; // ← importar ZoneId para manejar zonas horarias
import java.util.ArrayList;
import java.util.Optional;

/**
 * Adaptador JPA para el repositorio del carrito de compras.
 *
 * TIMEZONE FIX:
 *   fechaCreacion se genera con LocalDateTime.now(ZONA_BOGOTA)
 *   para que Oracle almacene la hora de Colombia (UTC-5)
 *   y no la hora UTC del servidor Docker.
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

    // ID del estado ACTIVO en la tabla ESTADOCARRITO
    private static final Long ESTADO_ACTIVO = 1L;

    // Zona horaria de Colombia — UTC-5, sin horario de verano
    // Se aplica a la fecha de creación del carrito para que quede en hora local
    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");

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
     * La fecha de creación se guarda en hora de Bogotá (UTC-5).
     */
    @Override
    public Carrito crearCarrito(Long idUsuario) {
        var entity = CarritoEntity.builder()
                .idUsuario(idUsuario)
                .idEstadoCarrito(ESTADO_ACTIVO)
                .fechaCreacion(LocalDateTime.now(ZONA_BOGOTA))
                .fechaActualizacion(LocalDateTime.now(ZONA_BOGOTA))
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
     * saveAndFlush + refresh garantizan que Hibernate lea los datos frescos
     * desde BD (incluyendo la relación producto) en vez de la entidad cacheada.
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

        // flush: escribe en BD inmediatamente dentro de la transacción
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
     * saveAndFlush() fuerza el UPDATE inmediato en BD dentro de la misma
     * transacción, garantizando que el carrito quede CONVERTIDO antes de
     * que el cliente reciba la respuesta del checkout.
     */
   @Override
    @Transactional
    public void cambiarEstado(Long idCarrito, Long idEstado) {
        carritoRepo.findById(idCarrito).ifPresent(c -> {
            c.setIdEstadoCarrito(idEstado);
            
            c.setFechaActualizacion(LocalDateTime.now(ZONA_BOGOTA));

            carritoRepo.saveAndFlush(c);

            log.debug("Carrito {} → estado {} (flush inmediato)", idCarrito, idEstado);
        });
    }
}