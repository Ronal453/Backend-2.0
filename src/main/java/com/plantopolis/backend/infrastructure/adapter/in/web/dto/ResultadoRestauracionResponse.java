package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import com.plantopolis.backend.domain.model.ResultadoRestauracion;
import java.time.LocalDateTime;
import java.util.List;

public record ResultadoRestauracionResponse(
        String nombreArchivo,
        LocalDateTime fechaRestauracion,
        List<TablaRespaldoResponse> tablas,
        long totalFilas,
        long duracionMs,
        List<String> advertencias
) {
    public static ResultadoRestauracionResponse from(ResultadoRestauracion rr) {
        return new ResultadoRestauracionResponse(
                rr.getNombreArchivo(),
                rr.getFechaRestauracion(),
                TablaRespaldoResponse.from(rr.getTablas()),
                rr.getTotalFilas(),
                rr.getDuracionMs(),
                rr.getAdvertencias()
        );
    }
}
