package com.plantopolis.backend.infrastructure.persistence.mapper;

import com.plantopolis.backend.domain.model.Zona;
import com.plantopolis.backend.infrastructure.persistence.entity.ZonaEntity;
import org.springframework.stereotype.Component;

@Component
public class ZonaMapper {

    public Zona toDomain(ZonaEntity entity) {
        if (entity == null) return null;
        return Zona.builder()
                .idZona(entity.getIdZona())
                .nombre(entity.getNombre())
                .capacidadMaxima(entity.getCapacidadMaxima())
                .tipoCondicion(entity.getTipoCondicion())
                .exposicionSolar(entity.getExposicionSolar())
                .activo(entity.getActivo())
                .fechaCreacion(entity.getFechaCreacion())
                .build();
    }

    public ZonaEntity toEntity(Zona domain) {
        if (domain == null) return null;
        return ZonaEntity.builder()
                .idZona(domain.getIdZona())
                .nombre(domain.getNombre())
                .capacidadMaxima(domain.getCapacidadMaxima())
                .tipoCondicion(domain.getTipoCondicion())
                .exposicionSolar(domain.getExposicionSolar())
                .activo(domain.getActivo() != null ? domain.getActivo() : true)
                .fechaCreacion(domain.getFechaCreacion())
                .build();
    }
}
