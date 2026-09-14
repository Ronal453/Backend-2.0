package com.plantopolis.backend.domain.port.in;

import com.plantopolis.backend.domain.model.CausaMerma;
import com.plantopolis.backend.domain.model.Merma;

import java.time.LocalDate;
import java.util.List;

public interface GestionarMermasTrabajadorUseCase {
    List<CausaMerma> listarCausasMerma();
    Merma registrarMerma(Long idLote, Long idCausa, Integer cantidadPerdida, LocalDate fechaMerma, String observaciones, String emailUsuario);
    List<Merma> listarMermasRecientes(int limite);
}
