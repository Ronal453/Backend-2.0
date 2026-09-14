package com.plantopolis.backend.domain.port.in;

import com.plantopolis.backend.domain.model.HistorialEstadoLote;
import com.plantopolis.backend.domain.model.LoteProduccion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GestionarLotesTrabajadorUseCase {
    Page<LoteProduccion> listarLotes(Long idZona, String estadoLote, Pageable pageable);
    LoteProduccion obtenerDetalleLote(Long idLote);
    LoteProduccion cambiarEstadoLote(Long idLote, String nuevoEstado, String observaciones, String emailUsuario);
    List<HistorialEstadoLote> obtenerHistorialLote(Long idLote);
}
