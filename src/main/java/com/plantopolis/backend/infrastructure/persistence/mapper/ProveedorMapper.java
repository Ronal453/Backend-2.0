package com.plantopolis.backend.infrastructure.persistence.mapper;

import com.plantopolis.backend.domain.model.Proveedor;
import com.plantopolis.backend.infrastructure.persistence.entity.ProveedorEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper manual entre el modelo de dominio {@link Proveedor} y la entidad JPA {@link ProveedorEntity}.
 */
@Component
public class ProveedorMapper {

    /**
     * @param entity entidad JPA (puede ser null)
     * @return modelo de dominio equivalente o null
     */
    public Proveedor toDomain(ProveedorEntity entity) {
        if (entity == null) return null;
        return Proveedor.builder()
                .idProveedor(entity.getIdProveedor())
                .nombre(entity.getNombre())
                .contacto(entity.getContacto())
                .telefono(entity.getTelefono())
                .correo(entity.getCorreo())
                .tipoInsumo(entity.getTipoInsumo())
                .activo(entity.getActivo())
                .fechaCreacion(entity.getFechaCreacion())
                .build();
    }

    /**
     * @param domain modelo de dominio (puede ser null)
     * @return entidad JPA lista para persistir; {@code activo} por defecto en true
     */
    public ProveedorEntity toEntity(Proveedor domain) {
        if (domain == null) return null;
        return ProveedorEntity.builder()
                .idProveedor(domain.getIdProveedor())
                .nombre(domain.getNombre())
                .contacto(domain.getContacto())
                .telefono(domain.getTelefono())
                .correo(domain.getCorreo())
                .tipoInsumo(domain.getTipoInsumo())
                .activo(domain.getActivo() != null ? domain.getActivo() : true)
                .fechaCreacion(domain.getFechaCreacion())
                .build();
    }
}
