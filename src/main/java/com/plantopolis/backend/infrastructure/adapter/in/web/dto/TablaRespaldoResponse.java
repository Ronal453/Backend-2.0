package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import com.plantopolis.backend.domain.model.TablaRespaldo;
import java.util.List;
import java.util.stream.Collectors;

public record TablaRespaldoResponse(
        String nombreTabla,
        long filas
) {
    public static TablaRespaldoResponse from(TablaRespaldo tr) {
        return new TablaRespaldoResponse(tr.getNombreTabla(), tr.getFilas());
    }
    public static List<TablaRespaldoResponse> from(List<TablaRespaldo> lista) {
        return lista.stream().map(TablaRespaldoResponse::from).collect(Collectors.toList());
    }
}
