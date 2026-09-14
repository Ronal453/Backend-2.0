package com.plantopolis.backend.domain.port.out;

import com.plantopolis.backend.domain.model.HistorialEstadoLote;

import java.util.List;

public interface HistorialEstadoLoteRepositoryPort {
    HistorialEstadoLote guardar(HistorialEstadoLote historial);
    List<HistorialEstadoLote> buscarPorIdLote(Long idLote);
}
