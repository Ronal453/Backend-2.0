package com.plantopolis.backend.domain.port.out;

import com.plantopolis.backend.domain.model.Categoria;
import com.plantopolis.backend.domain.model.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para operaciones de persistencia del catálogo de productos.
 */
public interface ProductoRepositoryPort {

    /**
     * Busca productos con filtros botánicos y comerciales.
     */
    Page<Producto> buscarConFiltros(
            String nombre, Long idCategoria,
            String luz, String riego,
            BigDecimal precioMin, BigDecimal precioMax, Pageable pageable
    );

    /**
     * Sobrecarga de compatibilidad hacia atrás sin filtros botánicos de luz y riego.
     */
    default Page<Producto> buscarConFiltros(
            String nombre, Long idCategoria,
            BigDecimal precioMin, BigDecimal precioMax, Pageable pageable) {
        return buscarConFiltros(nombre, idCategoria, null, null, precioMin, precioMax, pageable);
    }

    Optional<Producto> buscarPorId(Long id);

    List<Categoria> listarCategorias();

    Producto guardar(Producto producto);

    boolean existePorId(Long id);

    Page<Producto> buscarTodosAdmin(
            String nombre, Long idCategoria, Boolean activo, Pageable pageable
    );

    Long contarActivos();

    /** Productos activos en o por debajo del umbral de stock configurado. */
    List<Producto> listarConStockCritico();
}
