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
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CarritoJpaAdapter implements CarritoRepositoryPort {

    private final CarritoJpaRepository     carritoRepo;
    private final CarritoItemJpaRepository itemRepo;
    private final CarritoMapper            mapper;

    // EntityManager para poder hacer refresh y limpiar la caché de Hibernate
    @PersistenceContext
    private EntityManager entityManager;

    // ID de EstadoCarrito = 1 → ACTIVO (según datos iniciales del SQL)
    private static final Long ESTADO_ACTIVO = 1L;

    // Busca el carrito activo del usuario; devuelve vacío si no existe
    @Override
    public Optional<Carrito> buscarCarritoActivo(Long idUsuario) {
        return carritoRepo.findCarritoActivo(idUsuario)
                .map(mapper::toDomain);
    }

    // Crea un carrito nuevo en estado ACTIVO para el usuario
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

    // Busca un ítem del carrito por idCarrito + idProducto (para detectar duplicados)
    @Override
    public Optional<ItemCarrito> buscarItem(Long idCarrito, Long idProducto) {
        return itemRepo.findByIdCarritoAndIdProducto(idCarrito, idProducto)
                .map(mapper::itemToDomain);
    }

    // Busca un ítem directamente por su propio ID
    @Override
    public Optional<ItemCarrito> buscarItemPorId(Long idItem) {
        return itemRepo.findById(idItem)
                .map(mapper::itemToDomain);
    }

    // Persiste un ítem (insert o update) y devuelve el dominio con datos completos.
    //
    // PROBLEMA ORIGINAL: al construir CarritoItemEntity con .builder() la relación
    // @ManyToOne producto queda null. Hibernate la almacena así en su caché de
    // primer nivel (sesión actual). Cuando verCarrito() carga el carrito en la
    // MISMA transacción, obtiene el objeto cacheado → precio $0, nombre vacío.
    //
    // SOLUCIÓN: saveAndFlush() escribe en BD, luego entityManager.refresh()
    // descarta el objeto del caché y fuerza una lectura fresca desde BD,
    // disparando el EAGER fetch de ProductoEntity correctamente.
    @Override
    @Transactional
    public ItemCarrito guardarItem(ItemCarrito item) {
        var entity = CarritoItemEntity.builder()
                .idItem(item.getIdItem())
                .idCarrito(item.getIdCarrito())
                .idProducto(item.getIdProducto())
                .cantidad(item.getCantidad())
                .build();

        // Primero flush: asegura que el INSERT/UPDATE llegue a la BD
        var saved = itemRepo.saveAndFlush(entity);

        // Refresh: descarta la entidad del caché de Hibernate y la recarga
        // desde BD con todas las relaciones EAGER (producto incluido)
        entityManager.refresh(saved);

        return mapper.itemToDomain(saved);
    }

    // Elimina un ítem por su ID usando JPQL directo para evitar conflictos
    // con orphanRemoval = true del CarritoEntity padre
    @Override
    @Transactional
    public void eliminarItem(Long idItem) {
        itemRepo.deleteByIdItem(idItem);
    }

    // Vacía todos los ítems de un carrito (usado en checkout y "vaciar carrito")
    @Override
    @Transactional
    public void vaciarCarrito(Long idCarrito) {
        itemRepo.deleteByIdCarrito(idCarrito);
    }

    // Busca un carrito completo por su ID (usado en cambios de estado)
    @Override
    public Optional<Carrito> buscarPorId(Long idCarrito) {
        return carritoRepo.findById(idCarrito)
                .map(mapper::toDomain);
    }

    // Cambia el estado del carrito (ACTIVO → CONVERTIDO al hacer checkout)
    @Override
    public void cambiarEstado(Long idCarrito, Long idEstado) {
        carritoRepo.findById(idCarrito).ifPresent(c -> {
            c.setIdEstadoCarrito(idEstado);
            carritoRepo.save(c);
        });
    }
}