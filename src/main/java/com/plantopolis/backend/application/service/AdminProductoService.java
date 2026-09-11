package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.Producto;
import com.plantopolis.backend.domain.port.in.GestionarProductosAdminUseCase;
import com.plantopolis.backend.domain.port.out.ProductoRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminProductoService implements GestionarProductosAdminUseCase {

    private final ProductoRepositoryPort productoRepository;

    @Override
    public Page<Producto> listarTodos(String nombre, Long idCategoria,
                                      Long idTipo, Pageable pageable) {
        return productoRepository.buscarTodosAdmin(nombre, idCategoria, idTipo, pageable);
    }

    @Override
    @Transactional
    public Producto crear(Producto producto) {
        producto.setActivo(true);
        producto.setIdProducto(null);
        return productoRepository.guardar(producto);
    }

    @Override
    @Transactional
    public Producto actualizar(Long id, Producto datos) {
        var existente = productoRepository.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + id));

        if (datos.getNombreProducto() != null) existente.setNombreProducto(datos.getNombreProducto());
        if (datos.getDescripcion() != null) existente.setDescripcion(datos.getDescripcion());
        if (datos.getPrecio() != null) existente.setPrecio(datos.getPrecio());
        if (datos.getStock() != null) existente.setStock(datos.getStock());
        if (datos.getImagenUrl() != null) existente.setImagenUrl(datos.getImagenUrl());
        if (datos.getIdCategoria() != null) existente.setIdCategoria(datos.getIdCategoria());
        if (datos.getIdTipo() != null) existente.setIdTipo(datos.getIdTipo());
        if (datos.getCuidados() != null) existente.setCuidados(datos.getCuidados());
        if (datos.getLuz() != null) existente.setLuz(datos.getLuz());
        if (datos.getRiego() != null) existente.setRiego(datos.getRiego());
        if (datos.getTamanioEstimado() != null) existente.setTamanioEstimado(datos.getTamanioEstimado());
        if (datos.getStockMinimoAlerta() != null) existente.setStockMinimoAlerta(datos.getStockMinimoAlerta());

        return productoRepository.guardar(existente);
    }

    @Override
    @Transactional
    public Producto activar(Long id) {
        var producto = productoRepository.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + id));
        producto.setActivo(true);
        return productoRepository.guardar(producto);
    }

    @Override
    @Transactional
    public Producto desactivar(Long id) {
        var producto = productoRepository.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + id));
        producto.setActivo(false);
        return productoRepository.guardar(producto);
    }

    @Override
    public List<Producto> listarStockCritico() {
        return productoRepository.listarConStockCritico();
    }
}