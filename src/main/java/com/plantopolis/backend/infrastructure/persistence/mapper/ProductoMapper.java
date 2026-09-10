package com.plantopolis.backend.infrastructure.persistence.mapper;

import com.plantopolis.backend.domain.model.Categoria;
import com.plantopolis.backend.domain.model.Producto;
import com.plantopolis.backend.infrastructure.persistence.entity.CategoriaEntity;
import com.plantopolis.backend.infrastructure.persistence.entity.ProductoEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ProductoMapper {

    public Producto toDomain(ProductoEntity entity) {
        if (entity == null) return null;
        return Producto.builder()
                .idProducto(entity.getIdProducto())
                .idCategoria(entity.getIdCategoria())
                .idTipo(entity.getIdTipo())
                .nombreProducto(entity.getNombreProducto())
                .descripcion(entity.getDescripcion())
                .precio(entity.getPrecio())
                // NUEVO SCHEMA V2
                .porcentajeIva(entity.getPorcentajeIva())
                .stock(entity.getStock())
                // NUEVO SCHEMA V2
                .stockMinimoAlerta(entity.getStockMinimoAlerta())
                .imagenUrl(entity.getImagenUrl())
                .cuidados(entity.getCuidados())
                .luz(entity.getLuz())
                .riego(entity.getRiego())
                .tamanioEstimado(entity.getTamanioEstimado())
                .activo(entity.getActivo())
                .nombreCategoria(entity.getCategoria() != null
                        ? entity.getCategoria().getNombreCategoria() : null)
                .nombreTipo(entity.getTipo() != null
                        ? entity.getTipo().getNombreTipo() : null)
                .build();
    }

    public ProductoEntity toEntity(Producto domain) {
        if (domain == null) return null;
        return ProductoEntity.builder()
                .idProducto(domain.getIdProducto())
                .idCategoria(domain.getIdCategoria())
                .idTipo(domain.getIdTipo())
                .nombreProducto(domain.getNombreProducto())
                .descripcion(domain.getDescripcion())
                .precio(domain.getPrecio())
                .porcentajeIva(domain.getPorcentajeIva() != null
                        ? domain.getPorcentajeIva() : new BigDecimal("19.00"))
                .stock(domain.getStock())
                // default 5 si no se especifica
                .stockMinimoAlerta(domain.getStockMinimoAlerta() != null
                        ? domain.getStockMinimoAlerta() : 5)
                .imagenUrl(domain.getImagenUrl())
                .cuidados(domain.getCuidados())
                .luz(domain.getLuz())
                .riego(domain.getRiego())
                .tamanioEstimado(domain.getTamanioEstimado())
                .activo(domain.getActivo() != null ? domain.getActivo() : true)
                .build();
    }

    public Categoria categoriaToDomain(CategoriaEntity entity) {
        if (entity == null) return null;
        return Categoria.builder()
                .idCategoria(entity.getIdCategoria())
                .nombreCategoria(entity.getNombreCategoria())
                .descripcion(entity.getDescripcion())
                .build();
    }
}