package com.plantopolis.backend.domain.port.in;

import com.plantopolis.backend.domain.model.LoteProduccion;
import com.plantopolis.backend.domain.model.Proveedor;
import com.plantopolis.backend.domain.model.ReporteProveedor;

import java.util.List;

/**
 * Puerto de entrada (caso de uso) para la gestión administrativa de proveedores.
 * Cubre HU34 (alta, edición, activación/desactivación) y HU35 (reporte de lotes por proveedor).
 */
public interface GestionarProveedoresAdminUseCase {

    /**
     * Lista proveedores aplicando filtros opcionales.
     *
     * @param termino    texto a buscar en nombre o contacto (opcional)
     * @param tipoInsumo tipo de insumo exacto (opcional)
     * @param activo     estado de activación (opcional)
     * @return proveedores ordenados por nombre
     */
    List<Proveedor> listarProveedores(String termino, String tipoInsumo, Boolean activo);

    /** @return solo proveedores activos, usados en selectores al registrar lotes. */
    List<Proveedor> listarProveedoresActivos();

    /**
     * @param idProveedor identificador del proveedor
     * @return proveedor encontrado
     * @throws IllegalArgumentException si no existe
     */
    Proveedor obtenerProveedor(Long idProveedor);

    /** Registra un nuevo proveedor activo validando nombre único y tipo de insumo. */
    Proveedor crearProveedor(Proveedor proveedor);

    /** Actualiza los datos editables de un proveedor existente. */
    Proveedor actualizarProveedor(Long idProveedor, Proveedor proveedor);

    /** Marca el proveedor como activo. */
    Proveedor activarProveedor(Long idProveedor);

    /**
     * Marca el proveedor como inactivo. Su historial de lotes se conserva intacto,
     * pero ya no podrá asignarse a nuevos lotes.
     */
    Proveedor desactivarProveedor(Long idProveedor);

    /** HU35: lotes asociados a un proveedor (trazabilidad hacia atrás). */
    List<LoteProduccion> listarLotesPorProveedor(Long idProveedor);

    /** HU35: reporte agregado de lotes y pérdidas por cada proveedor registrado. */
    List<ReporteProveedor> generarReporteLotesPorProveedor();
}
