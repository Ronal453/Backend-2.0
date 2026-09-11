package com.plantopolis.backend.domain.port.out;

import com.plantopolis.backend.domain.model.Categoria;
import com.plantopolis.backend.domain.model.Producto;
import com.plantopolis.backend.domain.model.TipoProducto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductoRepositoryPort {

    Page<Producto> buscarConFiltros(
            String nombre, Long idCategoria, Long idTipo,
            BigDecimal precioMin, BigDecimal precioMax, Pageable pageable
    );

    Optional<Producto> buscarPorId(Long id);

    List<Categoria> listarCategorias();

    List<TipoProducto> listarTipos();

    Producto guardar(Producto producto);

    boolean existePorId(Long id);

    Page<Producto> buscarTodosAdmin(
            String nombre, Long idCategoria, Long idTipo, Pageable pageable
    );

    Long contarActivos();

    /** Productos activos en o por debajo del umbral de stock configurado. */
    List<Producto> listarConStockCritico();
}