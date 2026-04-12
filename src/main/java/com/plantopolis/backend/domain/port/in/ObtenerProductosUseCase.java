package com.plantopolis.backend.domain.port.in;

import com.plantopolis.backend.domain.model.Categoria;
import com.plantopolis.backend.domain.model.Producto;
import com.plantopolis.backend.domain.model.TipoProducto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface ObtenerProductosUseCase {

    Page<Producto> listarProductos(
            String nombre,
            Long idCategoria,
            Long idTipo,
            BigDecimal precioMin,
            BigDecimal precioMax,
            Pageable pageable
    );

    Producto obtenerDetalle(Long id);

    List<Categoria> listarCategorias();

    List<TipoProducto> listarTipos();   // ← nuevo
}