package com.plantopolis.backend.domain.port.out;

import com.plantopolis.backend.domain.model.Zona;

import java.util.List;
import java.util.Optional;

public interface ZonaRepositoryPort {
    List<Zona> listarActivas();
    Optional<Zona> buscarPorId(Long idZona);
}
