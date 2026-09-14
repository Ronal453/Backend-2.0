package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.Zona;
import com.plantopolis.backend.domain.port.in.ObtenerZonasUseCase;
import com.plantopolis.backend.domain.port.out.ZonaRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ZonaService implements ObtenerZonasUseCase {

    private final ZonaRepositoryPort zonaRepository;

    @Override
    public List<Zona> listarZonasActivas() {
        return zonaRepository.listarActivas();
    }
}
