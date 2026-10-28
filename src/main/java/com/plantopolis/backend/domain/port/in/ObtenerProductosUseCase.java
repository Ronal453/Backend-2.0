package com.plantopolis.backend.domain.port.in;

import com.plantopolis.backend.domain.model.Categoria;
import com.plantopolis.backend.domain.model.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

/**
 * Caso de uso para consultar el catálogo de productos y sus metadatos botánicos.
 */
public interface ObtenerProductosUseCase {

    /**
     * Lista productos con filtros de nombre, categoría, requerimiento de luz y frecuencia de riego.
     */
    Page<Producto> listarProductos(
            String nombre,
            Long idCategoria,
            String luz,
            String riego,
            BigDecimal precioMin,
            BigDecimal precioMax,
            Pageable pageable
    );

    /**
     * Sobrecarga de compatibilidad hacia atrás sin filtros botánicos.
     */
    default Page<Producto> listarProductos(
            String nombre,
            Long idCategoria,
            BigDecimal precioMin,
            BigDecimal precioMax,
            Pageable pageable) {
        return listarProductos(nombre, idCategoria, null, null, precioMin, precioMax, pageable);
    }

    Producto obtenerDetalle(Long id);

    List<Categoria> listarCategorias();
}