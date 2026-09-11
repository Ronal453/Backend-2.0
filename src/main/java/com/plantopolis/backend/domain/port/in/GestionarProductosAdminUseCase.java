package com.plantopolis.backend.domain.port.in;

import com.plantopolis.backend.domain.model.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GestionarProductosAdminUseCase {

    Page<Producto> listarTodos(String nombre, Long idCategoria,
                               Long idTipo, Pageable pageable);

    Producto crear(Producto producto);

    Producto actualizar(Long id, Producto datos);

    Producto activar(Long id);

    Producto desactivar(Long id);

    /** Productos activos cuyo stock está en o por debajo del umbral configurado. */
    List<Producto> listarStockCritico();
}