package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.Categoria;
import com.plantopolis.backend.domain.model.Producto;
import com.plantopolis.backend.domain.model.TipoProducto;
import com.plantopolis.backend.domain.port.in.ObtenerProductosUseCase;
import com.plantopolis.backend.domain.port.out.ProductoRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoService implements ObtenerProductosUseCase {

    private final ProductoRepositoryPort productoRepository;

    @Override
    public Page<Producto> listarProductos(
            String nombre, Long idCategoria, Long idTipo,
            BigDecimal precioMin, BigDecimal precioMax, Pageable pageable) {

        return productoRepository.buscarConFiltros(
                nombre, idCategoria, idTipo, precioMin, precioMax, pageable);
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

    @Override
    public List<TipoProducto> listarTipos() {       // ← nuevo
        return productoRepository.listarTipos();
    }
}