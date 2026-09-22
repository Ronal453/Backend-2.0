package com.plantopolis.backend.domain.port.in;

import com.plantopolis.backend.domain.model.Zona;

public interface GestionarZonasAdminUseCase {
    Zona crearZona(Zona zona);
    Zona actualizarZona(Long idZona, Zona zona, boolean confirmarReduccionCapacidad);
    Zona activarZona(Long idZona);
    Zona desactivarZona(Long idZona);
}
