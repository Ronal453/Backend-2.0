package com.plantopolis.backend.infrastructure.adapter.out.persistence;

import com.plantopolis.backend.domain.model.Categoria;
import com.plantopolis.backend.domain.model.Producto;
import com.plantopolis.backend.domain.port.out.ProductoRepositoryPort;
import com.plantopolis.backend.infrastructure.persistence.mapper.ProductoMapper;
import com.plantopolis.backend.infrastructure.persistence.repository.CategoriaJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.repository.ProductoJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador de persistencia JPA para el catálogo de productos.
 * Implementa el puerto ProductoRepositoryPort conectando el dominio con los repositorios Spring Data.
 */
@Component
@RequiredArgsConstructor
public class ProductoJpaAdapter implements ProductoRepositoryPort {

    private final ProductoJpaRepository     productoRepo;
    private final CategoriaJpaRepository    categoriaRepo;
    private final ProductoMapper            mapper;

    @Override
    public Page<Producto> buscarConFiltros(
            String nombre, Long idCategoria,
            BigDecimal precioMin, BigDecimal precioMax, Pageable pageable) {
        return buscarConFiltros(nombre, idCategoria, null, null, precioMin, precioMax, pageable);
    }

    @Override
    public Page<Producto> buscarConFiltros(
            String nombre, Long idCategoria,
            String luz, String riego,
            BigDecimal precioMin, BigDecimal precioMax, Pageable pageable) {
        return productoRepo
                .buscarConFiltros(nombre, idCategoria, luz, riego, precioMin, precioMax, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Producto> buscarPorId(Long id) {
        return productoRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Categoria> listarCategorias() {
        return categoriaRepo.findDistinctByNombreCategoria()
                .stream().map(mapper::categoriaToDomain).toList();
    }

    @Override
    public Producto guardar(Producto producto) {
        var entity = mapper.toEntity(producto);
        return mapper.toDomain(productoRepo.save(entity));
    }

    @Override
    public boolean existePorId(Long id) {
        return productoRepo.existsById(id);
    }

    @Override
    public Page<Producto> buscarTodosAdmin(
            String nombre, Long idCategoria, Boolean activo, Pageable pageable) {
        return productoRepo
                .buscarTodosAdmin(nombre, idCategoria, activo, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Long contarActivos() {
        return productoRepo.countByActivoTrue();
    }

    @Override
    public List<Producto> listarConStockCritico() {
        return productoRepo.buscarConStockCritico()
                .stream().map(mapper::toDomain).toList();
    }
}