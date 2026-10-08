package com.plantopolis.backend.infrastructure.adapter.out.persistence;

import com.plantopolis.backend.domain.model.Proveedor;
import com.plantopolis.backend.domain.port.out.ProveedorRepositoryPort;
import com.plantopolis.backend.infrastructure.persistence.mapper.ProveedorMapper;
import com.plantopolis.backend.infrastructure.persistence.repository.ProveedorJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador de salida JPA que implementa {@link ProveedorRepositoryPort}.
 * Traduce entre entidades JPA y modelos de dominio mediante {@link ProveedorMapper}.
 */
@Component
@RequiredArgsConstructor
public class ProveedorJpaAdapter implements ProveedorRepositoryPort {

    private final ProveedorJpaRepository proveedorRepo;
    private final ProveedorMapper mapper;

    @Override
    public Proveedor guardar(Proveedor proveedor) {
        return mapper.toDomain(proveedorRepo.save(mapper.toEntity(proveedor)));
    }

    @Override
    public Optional<Proveedor> buscarPorId(Long idProveedor) {
        return proveedorRepo.findById(idProveedor).map(mapper::toDomain);
    }

    @Override
    public List<Proveedor> listarTodos() {
        return proveedorRepo.findAllByOrderByNombreAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Proveedor> listarActivos() {
        return proveedorRepo.findByActivoTrueOrderByNombreAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Proveedor> buscarConFiltros(String termino, String tipoInsumo, Boolean activo) {
        return proveedorRepo.buscarConFiltros(termino, tipoInsumo, activo).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existeProveedorPorNombre(String nombre) {
        return proveedorRepo.existsByNombreIgnoreCase(nombre);
    }

    @Override
    public boolean existeProveedorPorNombreYDistintoId(String nombre, Long idProveedor) {
        return proveedorRepo.existsByNombreIgnoreCaseAndIdProveedorNot(nombre, idProveedor);
    }
}
