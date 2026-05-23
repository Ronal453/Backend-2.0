package com.plantopolis.backend.infrastructure.adapter.out.persistence;

import com.plantopolis.backend.domain.model.Categoria;
import com.plantopolis.backend.domain.model.Producto;
import com.plantopolis.backend.domain.model.TipoProducto;
import com.plantopolis.backend.domain.port.out.ProductoRepositoryPort;
import com.plantopolis.backend.infrastructure.persistence.mapper.ProductoMapper;
import com.plantopolis.backend.infrastructure.persistence.repository.CategoriaJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.repository.ProductoJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.repository.TipoProductoJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductoJpaAdapter implements ProductoRepositoryPort {

    private final ProductoJpaRepository     productoRepo;
    private final CategoriaJpaRepository    categoriaRepo;
    private final TipoProductoJpaRepository tipoRepo;
    private final ProductoMapper            mapper;

    @Override
    public Page<Producto> buscarConFiltros(
            String nombre, Long idCategoria, Long idTipo,
            BigDecimal precioMin, BigDecimal precioMax, Pageable pageable) {
        return productoRepo
                .buscarConFiltros(nombre, idCategoria, idTipo,
                                  precioMin, precioMax, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Producto> buscarPorId(Long id) {
        return productoRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Categoria> listarCategorias() {
        return categoriaRepo.findDistinctByNombreCategoria()
                .stream()
                .map(mapper::categoriaToDomain)
                .toList();
    }

    @Override
    public List<TipoProducto> listarTipos() {
        return tipoRepo.findAll()
                .stream()
                .map(t -> TipoProducto.builder()
                        .idTipo(t.getIdTipo())
                        .nombreTipo(t.getNombreTipo())
                        .build())
                .toList();
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

    /**
     * Lista todos los productos (activos + inactivos) para el admin.
     * Usa buscarTodosAdmin() que NO filtra por activo = true.
     */
    @Override
    public Page<Producto> buscarTodosAdmin(
            String nombre, Long idCategoria, Long idTipo, Pageable pageable) {
        return productoRepo
                .buscarTodosAdmin(nombre, idCategoria, idTipo, pageable)
                .map(mapper::toDomain);
    }

    /**
     * Cuenta los productos activos para el dashboard de reportes.
     */
    @Override
    public Long contarActivos() {
        return productoRepo.countByActivoTrue();
    }
}