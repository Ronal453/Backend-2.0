package com.plantopolis.backend.domain.port.out;

import com.plantopolis.backend.domain.model.CausaMerma;

import java.util.List;
import java.util.Optional;

public interface CausaMermaRepositoryPort {
    List<CausaMerma> listarActivas();
    Optional<CausaMerma> buscarPorId(Long idCausa);
}
