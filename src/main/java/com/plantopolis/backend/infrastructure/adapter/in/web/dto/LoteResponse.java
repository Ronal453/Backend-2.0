package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import com.plantopolis.backend.domain.model.LoteProduccion;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LoteResponse(
        Long idLote,
        String codigoLote,
        String especie,
        Integer cantidadInicial,
        Integer cantidadActual,
        LocalDate fechaSiembra,
        String estadoLote,
        Long idZona,
        String nombreZona,
        Long idProveedor,
        String nombreProveedor,
        Long idProducto,
        String nombreProducto,
        Boolean esVinculado,
        LocalDateTime fechaVinculacion,
        LocalDateTime fechaCreacion
) {
    public static LoteResponse from(LoteProduccion l) {
        if (l == null) return null;
        return new LoteResponse(
                l.getIdLote(),
                l.getCodigoLote(),
                l.getEspecie(),
                l.getCantidadInicial(),
                l.getCantidadActual(),
                l.getFechaSiembra(),
                l.getEstadoLote(),
                l.getIdZona(),
                l.getNombreZona(),
                l.getIdProveedor(),
                l.getNombreProveedor(),
                l.getIdProducto(),
                l.getNombreProducto(),
                l.getEsVinculado(),
                l.getFechaVinculacion(),
                l.getFechaCreacion()
        );
    }
}
