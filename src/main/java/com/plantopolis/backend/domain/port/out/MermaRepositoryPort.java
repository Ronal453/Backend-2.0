package com.plantopolis.backend.domain.port.out;

import com.plantopolis.backend.domain.model.Merma;

import java.util.List;

public interface MermaRepositoryPort {
    Merma guardar(Merma merma);
    List<Merma> listarRecientes(int limite);
}
