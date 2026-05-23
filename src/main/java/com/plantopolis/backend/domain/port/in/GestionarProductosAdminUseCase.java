package com.plantopolis.backend.domain.port.in;

import com.plantopolis.backend.domain.model.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Operaciones disponibles para el ADMINISTRADOR:
 *   - Listar TODOS los productos (activos e inactivos)
 *   - Crear producto nuevo
 *   - Actualizar datos de un producto existente
 *   - Activar o desactivar un producto del catálogo
 *
 */
public interface GestionarProductosAdminUseCase {

    /**
     * Lista todos los productos (incluyendo inactivos) para el panel admin.
     * A diferencia del catálogo público, aquí se muestran todos.
     *
     * @param nombre      filtro parcial por nombre (puede ser null)
     * @param idCategoria filtro por categoría (puede ser null)
     * @param idTipo      filtro por tipo (puede ser null)
     * @param pageable    paginación y ordenamiento
     * @return página de productos
     */
    Page<Producto> listarTodos(String nombre, Long idCategoria,
                               Long idTipo, Pageable pageable);

    /**
     * Crea un nuevo producto en el sistema.
     * El producto queda activo por defecto.
     *
     * @param producto datos del nuevo producto (sin idProducto)
     * @return el producto creado con su ID asignado por la BD
     */
    Producto crear(Producto producto);

    /**
     * Actualiza los datos de un producto existente.
     * Solo actualiza los campos enviados; el stock y el estado activo
     * se mantienen a menos que se especifiquen explícitamente.
     *
     * @param id      ID del producto a actualizar
     * @param datos   objeto con los nuevos valores
     * @return el producto actualizado
     */
    Producto actualizar(Long id, Producto datos);

    /**
     * Activa un producto: lo hace visible en el catálogo público.
     * Cambia activo = true en la BD.
     *
     * @param id ID del producto a activar
     * @return el producto con activo = true
     */
    Producto activar(Long id);

    /**
     * Desactiva un producto: lo oculta del catálogo público.
     * El producto sigue en BD para preservar el historial de pedidos.
     * Cambia activo = false en la BD.
     *
     * @param id ID del producto a desactivar
     * @return el producto con activo = false
     */
    Producto desactivar(Long id);
}