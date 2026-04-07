package com.plantopolis.backend.infrastructure.persistence.mapper;

import com.plantopolis.backend.domain.model.Categoria;
import com.plantopolis.backend.domain.model.Producto;
import com.plantopolis.backend.infrastructure.persistence.entity.CategoriaEntity;
import com.plantopolis.backend.infrastructure.persistence.entity.ProductoEntity;
import org.springframework.stereotype.Component;

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
                .stock(entity.getStock())
                .imagenUrl(entity.getImagenUrl())
                .cuidados(entity.getCuidados())
                .luz(entity.getLuz())
                .riego(entity.getRiego())
                .tamanioEstimado(entity.getTamanioEstimado())
                .activo(entity.getActivo())
                // Nombres enriquecidos desde relaciones
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
                .stock(domain.getStock())
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
