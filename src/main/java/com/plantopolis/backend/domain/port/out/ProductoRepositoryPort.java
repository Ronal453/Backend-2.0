package com.plantopolis.backend.domain.port.out;

import com.plantopolis.backend.domain.model.Categoria;
import com.plantopolis.backend.domain.model.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductoRepositoryPort {

    // Catálogo con filtros y paginación
    Page<Producto> buscarConFiltros(
            String nombre,
            Long idCategoria,
            Long idTipo,
            BigDecimal precioMin,
            BigDecimal precioMax,
            Pageable pageable
    );

    // Detalle de un producto
    Optional<Producto> buscarPorId(Long id);

    // Listar categorías
    List<Categoria> listarCategorias();

    // Admin: guardar/actualizar
    Producto guardar(Producto producto);

    // Admin: verificar existencia
    boolean existePorId(Long id);
}