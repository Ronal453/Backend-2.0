package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import com.plantopolis.backend.domain.model.LoteProduccion;
import com.plantopolis.backend.domain.model.Zona;

import java.time.LocalDateTime;
import java.util.List;

public record ZonaDetalleResponse(
        Long idZona,
        String nombre,
        Integer capacidadMaxima,
        String tipoCondicion,
        String exposicionSolar,
        Boolean activo,
        List<LoteResumen> lotes
) {
    public static ZonaDetalleResponse from(Zona zona, List<LoteProduccion> lotes) {
        return new ZonaDetalleResponse(
                zona.getIdZona(),
                zona.getNombre(),
                zona.getCapacidadMaxima(),
                zona.getTipoCondicion(),
                zona.getExposicionSolar(),
                zona.getActivo(),
                lotes.stream().map(LoteResumen::from).toList()
        );
    }

    public record LoteResumen(
            Long idLote,
            String codigoLote,
            String especie,
            String estadoLote,
            Integer cantidadActual,
            Integer cantidadInicial,
            java.time.LocalDate fechaSiembra
    ) {
        public static LoteResumen from(LoteProduccion lote) {
            return new LoteResumen(
                    lote.getIdLote(),
                    lote.getCodigoLote(),
                    lote.getEspecie(),
                    lote.getEstadoLote(),
                    lote.getCantidadActual(),
                    lote.getCantidadInicial(),
                    lote.getFechaSiembra()
            );
        }
    }
}
