package com.plantopolis.backend.infrastructure.adapter.out.persistence;

import com.plantopolis.backend.domain.model.Carrito;
import com.plantopolis.backend.domain.model.ItemCarrito;
import com.plantopolis.backend.domain.port.out.CarritoRepositoryPort;
import com.plantopolis.backend.infrastructure.persistence.entity.CarritoEntity;
import com.plantopolis.backend.infrastructure.persistence.entity.CarritoItemEntity;
import com.plantopolis.backend.infrastructure.persistence.mapper.CarritoMapper;
import com.plantopolis.backend.infrastructure.persistence.repository.CarritoItemJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.repository.CarritoJpaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CarritoJpaAdapter implements CarritoRepositoryPort {

    private final CarritoJpaRepository carritoRepo;
    private final CarritoItemJpaRepository itemRepo;
    private final CarritoMapper mapper;

    // ID_ESTADO_CARRITO = 1 → ACTIVO
    private static final Long ESTADO_ACTIVO = 1L;

    @Override
    public Optional<Carrito> buscarCarritoActivo(Long idUsuario) {
        return carritoRepo.findCarritoActivo(idUsuario)
                .map(mapper::toDomain);
    }

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

    @Override
    public Optional<ItemCarrito> buscarItem(Long idCarrito, Long idProducto) {
        return itemRepo.findByIdCarritoAndIdProducto(idCarrito, idProducto)
                .map(mapper::itemToDomain);
    }

    @Override
    public ItemCarrito guardarItem(ItemCarrito item) {
        var entity = CarritoItemEntity.builder()
                .idItem(item.getIdItem())
                .idCarrito(item.getIdCarrito())
                .idProducto(item.getIdProducto())
                .cantidad(item.getCantidad())
                .build();
        return mapper.itemToDomain(itemRepo.save(entity));
    }

    @Override
    public void eliminarItem(Long idItem) {
        itemRepo.deleteById(idItem);
    }

    @Override
    @Transactional
    public void vaciarCarrito(Long idCarrito) {
        itemRepo.deleteByIdCarrito(idCarrito);
    }

    @Override
    public Optional<Carrito> buscarPorId(Long idCarrito) {
        return carritoRepo.findById(idCarrito)
                .map(mapper::toDomain);
    }

    @Override
    public void cambiarEstado(Long idCarrito, Long idEstado) {
        carritoRepo.findById(idCarrito).ifPresent(c -> {
            c.setIdEstadoCarrito(idEstado);
            carritoRepo.save(c);
        });
    }
}