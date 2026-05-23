package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.Producto;
import com.plantopolis.backend.domain.port.in.GestionarProductosAdminUseCase;
import com.plantopolis.backend.domain.port.out.ProductoRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 
 * Implementa las operaciones CRUD del panel admin:
 *   - Lista todos los productos (activos + inactivos)
 *   - Crea nuevos productos
 *   - Actualiza datos de productos existentes
 *   - Activa / desactiva productos del catálogo
 *
 */
@Service
@RequiredArgsConstructor
public class AdminProductoService implements GestionarProductosAdminUseCase {

    // Puerto de salida para acceder al repositorio de productos
    private final ProductoRepositoryPort productoRepository;

    /**
     * Lista todos los productos del sistema para el admin.
     * Incluye activos e inactivos (sin filtro por activo = true).
     */
    @Override
    public Page<Producto> listarTodos(String nombre, Long idCategoria,
                                      Long idTipo, Pageable pageable) {
        // Usa el método admin que no filtra por activo
        return productoRepository.buscarTodosAdmin(
                nombre, idCategoria, idTipo, pageable);
    }

    /**
     * Crea un nuevo producto en el sistema.
     *
     * Validaciones:
     *   - El nombre no puede estar vacío (viene del @Valid del controller)
     *   - El precio debe ser positivo (idem)
     *   - El stock no puede ser negativo (idem)
     *
     * El producto se crea con activo = true por defecto.
     */
    @Override
    @Transactional
    public Producto crear(Producto producto) {
        // Asegurar que el producto nuevo comience activo
        // aunque el request no lo especifique explícitamente
        producto.setActivo(true);

        // Limpiar el ID por si el request lo envía (es un POST, no PUT)
        producto.setIdProducto(null);

        return productoRepository.guardar(producto);
    }

    /**
     * Actualiza los datos de un producto existente.
     *
     * Estrategia de actualización:
     *   1. Cargar el producto existente desde la BD
     *   2. Sobreescribir los campos enviados en la request
     *   3. Preservar campos no enviados (activo, etc.)
     *   4. Guardar la versión actualizada
     *
     * @param id    ID del producto a actualizar
     * @param datos objeto con los nuevos valores del producto
     */
    @Override
    @Transactional
    public Producto actualizar(Long id, Producto datos) {
        // 1. Verificar que el producto existe
        var existente = productoRepository.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException(
                        "Producto no encontrado con id: " + id));

        // 2. Actualizar solo los campos que se envíen (no null)
        //    Esto permite actualizaciones parciales desde el frontend

        if (datos.getNombreProducto() != null)
            existente.setNombreProducto(datos.getNombreProducto());

        if (datos.getDescripcion() != null)
            existente.setDescripcion(datos.getDescripcion());

        if (datos.getPrecio() != null)
            existente.setPrecio(datos.getPrecio());

        if (datos.getStock() != null)
            existente.setStock(datos.getStock());

        if (datos.getImagenUrl() != null)
            existente.setImagenUrl(datos.getImagenUrl());

        if (datos.getIdCategoria() != null)
            existente.setIdCategoria(datos.getIdCategoria());

        if (datos.getIdTipo() != null)
            existente.setIdTipo(datos.getIdTipo());

        if (datos.getCuidados() != null)
            existente.setCuidados(datos.getCuidados());

        if (datos.getLuz() != null)
            existente.setLuz(datos.getLuz());

        if (datos.getRiego() != null)
            existente.setRiego(datos.getRiego());

        if (datos.getTamanioEstimado() != null)
            existente.setTamanioEstimado(datos.getTamanioEstimado());

        // 3. Guardar la versión actualizada
        return productoRepository.guardar(existente);
    }

    /**
     * Activa un producto: lo hace visible en el catálogo público.
     * Cambia activo = true en la BD.
     *
     * @param id ID del producto a activar
     */
    @Override
    @Transactional
    public Producto activar(Long id) {
        // Cargar producto (valida que exista)
        var producto = productoRepository.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException(
                        "Producto no encontrado con id: " + id));

        // Cambiar el flag activo y guardar
        producto.setActivo(true);
        return productoRepository.guardar(producto);
    }

    /**
     * Desactiva un producto: lo oculta del catálogo público.
     * El producto sigue en BD para preservar el historial.
     * Cambia activo = false en la BD.
     *
     * @param id ID del producto a desactivar
     */
    @Override
    @Transactional
    public Producto desactivar(Long id) {
        // Cargar producto (valida que exista)
        var producto = productoRepository.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException(
                        "Producto no encontrado con id: " + id));

        // Cambiar el flag activo y guardar
        producto.setActivo(false);
        return productoRepository.guardar(producto);
    }
}