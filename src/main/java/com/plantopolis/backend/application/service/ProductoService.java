package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.Categoria;
import com.plantopolis.backend.domain.model.Producto;
import com.plantopolis.backend.domain.port.in.ObtenerProductosUseCase;
import com.plantopolis.backend.domain.port.out.ProductoRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Servicio de aplicación para la consulta y catálogo de productos públicos.
 * Implementa el caso de uso ObtenerProductosUseCase.
 */
@Service
@RequiredArgsConstructor
public class ProductoService implements ObtenerProductosUseCase {

    private final ProductoRepositoryPort productoRepository;

    @Override
    public Page<Producto> listarProductos(
            String nombre, Long idCategoria,
            BigDecimal precioMin, BigDecimal precioMax, Pageable pageable) {

        return productoRepository.buscarConFiltros(
                nombre, idCategoria, precioMin, precioMax, pageable);
    }

    @Override
    public Page<Producto> listarProductos(
            String nombre, Long idCategoria,
            String luz, String riego,
            BigDecimal precioMin, BigDecimal precioMax, Pageable pageable) {

        return productoRepository.buscarConFiltros(
                nombre, idCategoria, luz, riego, precioMin, precioMax, pageable);
    }

    @Override
    public Producto obtenerDetalle(Long id) {
        return productoRepository.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException(
                        "Producto no encontrado con id: " + id));
    }

    @Override
    public List<Categoria> listarCategorias() {
        return productoRepository.listarCategorias();
    }
}