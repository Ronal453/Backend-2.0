package com.plantopolis.backend.domain.port.out;

import com.plantopolis.backend.domain.model.Proveedor;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida hacia la persistencia de proveedores (tabla PROVEEDOR).
 * Implementado por {@code ProveedorJpaAdapter}.
 */
public interface ProveedorRepositoryPort {
    Proveedor guardar(Proveedor proveedor);
    Optional<Proveedor> buscarPorId(Long idProveedor);
    List<Proveedor> listarTodos();
    List<Proveedor> listarActivos();
    List<Proveedor> buscarConFiltros(String termino, String tipoInsumo, Boolean activo);
    boolean existeProveedorPorNombre(String nombre);
    boolean existeProveedorPorNombreYDistintoId(String nombre, Long idProveedor);
}
