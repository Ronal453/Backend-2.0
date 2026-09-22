package com.plantopolis.backend.domain.port.in;

import com.plantopolis.backend.domain.model.Zona;

import java.util.List;

public interface ObtenerZonasUseCase {
    List<Zona> listarZonasActivas();
    List<Zona> listarTodasLasZonas();
    List<Zona> obtenerResumenOcupacion();
}
