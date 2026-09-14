package com.plantopolis.backend.infrastructure.persistence.mapper;

import com.plantopolis.backend.domain.model.CausaMerma;
import com.plantopolis.backend.domain.model.Merma;
import com.plantopolis.backend.infrastructure.persistence.entity.CausaMermaEntity;
import com.plantopolis.backend.infrastructure.persistence.entity.MermaEntity;
import org.springframework.stereotype.Component;

@Component
public class MermaMapper {

    public CausaMerma toDomainCausa(CausaMermaEntity entity) {
        if (entity == null) return null;
        return CausaMerma.builder()
                .idCausa(entity.getIdCausa())
                .nombreCausa(entity.getNombreCausa())
                .activo(entity.getActivo())
                .build();
    }

    public Merma toDomain(MermaEntity entity) {
        if (entity == null) return null;
        return Merma.builder()
                .idMerma(entity.getIdMerma())
                .idLote(entity.getIdLote())
                .codigoLote(entity.getLote() != null ? entity.getLote().getCodigoLote() : null)
                .especieLote(entity.getLote() != null ? entity.getLote().getEspecie() : null)
                .idCausa(entity.getIdCausa())
                .nombreCausa(entity.getCausa() != null ? entity.getCausa().getNombreCausa() : null)
                .idUsuario(entity.getIdUsuario())
                .nombreUsuario(entity.getUsuario() != null ? entity.getUsuario().getNombreCompleto() : null)
                .cantidadPerdida(entity.getCantidadPerdida())
                .fechaMerma(entity.getFechaMerma())
                .observaciones(entity.getObservaciones())
                .fechaRegistro(entity.getFechaRegistro())
                .build();
    }

    public MermaEntity toEntity(Merma domain) {
        if (domain == null) return null;
        return MermaEntity.builder()
                .idMerma(domain.getIdMerma())
                .idLote(domain.getIdLote())
                .idCausa(domain.getIdCausa())
                .idUsuario(domain.getIdUsuario())
                .cantidadPerdida(domain.getCantidadPerdida())
                .fechaMerma(domain.getFechaMerma())
                .observaciones(domain.getObservaciones())
                .fechaRegistro(domain.getFechaRegistro())
                .build();
    }
}
