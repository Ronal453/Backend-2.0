package com.plantopolis.backend.infrastructure.persistence.mapper;

import com.plantopolis.backend.domain.model.HistorialEstadoLote;
import com.plantopolis.backend.domain.model.LoteProduccion;
import com.plantopolis.backend.infrastructure.persistence.entity.HistorialEstadoLoteEntity;
import com.plantopolis.backend.infrastructure.persistence.entity.LoteProduccionEntity;
import org.springframework.stereotype.Component;

@Component
public class LoteMapper {

    public LoteProduccion toDomain(LoteProduccionEntity entity) {
        if (entity == null) return null;
        return LoteProduccion.builder()
                .idLote(entity.getIdLote())
                .codigoLote(entity.getCodigoLote())
                .especie(entity.getEspecie())
                .cantidadInicial(entity.getCantidadInicial())
                .cantidadActual(entity.getCantidadActual())
                .fechaSiembra(entity.getFechaSiembra())
                .estadoLote(entity.getEstadoLote())
                .idZona(entity.getIdZona())
                .nombreZona(entity.getZona() != null ? entity.getZona().getNombre() : null)
                .idProveedor(entity.getIdProveedor())
                .idProducto(entity.getIdProducto())
                .nombreProducto(entity.getProducto() != null ? entity.getProducto().getNombreProducto() : null)
                .esVinculado(entity.getEsVinculado())
                .fechaVinculacion(entity.getFechaVinculacion())
                .fechaCreacion(entity.getFechaCreacion())
                .build();
    }

    public LoteProduccionEntity toEntity(LoteProduccion domain) {
        if (domain == null) return null;
        return LoteProduccionEntity.builder()
                .idLote(domain.getIdLote())
                .codigoLote(domain.getCodigoLote())
                .especie(domain.getEspecie())
                .cantidadInicial(domain.getCantidadInicial())
                .cantidadActual(domain.getCantidadActual())
                .fechaSiembra(domain.getFechaSiembra())
                .estadoLote(domain.getEstadoLote())
                .idZona(domain.getIdZona())
                .idProveedor(domain.getIdProveedor())
                .idProducto(domain.getIdProducto())
                .esVinculado(domain.getEsVinculado() != null ? domain.getEsVinculado() : false)
                .fechaVinculacion(domain.getFechaVinculacion())
                .fechaCreacion(domain.getFechaCreacion())
                .build();
    }

    public HistorialEstadoLote toDomainHistorial(HistorialEstadoLoteEntity entity) {
        if (entity == null) return null;
        return HistorialEstadoLote.builder()
                .idHistorialLote(entity.getIdHistorialLote())
                .idLote(entity.getIdLote())
                .idUsuario(entity.getIdUsuario())
                .nombreUsuario(entity.getUsuario() != null ? entity.getUsuario().getNombreCompleto() : null)
                .estadoAnterior(entity.getEstadoAnterior())
                .estadoNuevo(entity.getEstadoNuevo())
                .observaciones(entity.getObservaciones())
                .fechaCambio(entity.getFechaCambio())
                .build();
    }

    public HistorialEstadoLoteEntity toEntityHistorial(HistorialEstadoLote domain) {
        if (domain == null) return null;
        return HistorialEstadoLoteEntity.builder()
                .idHistorialLote(domain.getIdHistorialLote())
                .idLote(domain.getIdLote())
                .idUsuario(domain.getIdUsuario())
                .estadoAnterior(domain.getEstadoAnterior())
                .estadoNuevo(domain.getEstadoNuevo())
                .observaciones(domain.getObservaciones())
                .fechaCambio(domain.getFechaCambio())
                .build();
    }
}
