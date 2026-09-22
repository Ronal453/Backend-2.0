package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.Zona;
import com.plantopolis.backend.domain.port.in.GestionarZonasAdminUseCase;
import com.plantopolis.backend.domain.port.in.ObtenerZonasUseCase;
import com.plantopolis.backend.domain.port.out.LoteRepositoryPort;
import com.plantopolis.backend.domain.port.out.ZonaRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ZonaService implements ObtenerZonasUseCase, GestionarZonasAdminUseCase {

    private final ZonaRepositoryPort zonaRepository;
    private final LoteRepositoryPort loteRepository;

    @Override
    public List<Zona> listarZonasActivas() {
        return zonaRepository.listarActivas();
    }

    @Override
    public List<Zona> listarTodasLasZonas() {
        return zonaRepository.listarTodas();
    }

    @Override
    public List<Zona> obtenerResumenOcupacion() {
        // As per instructions, this returns the active zones.
        // Occupancy count is computed in the controller or service layer wrapping it.
        return zonaRepository.listarActivas();
    }

    @Override
    public Zona crearZona(Zona zona) {
        if (zonaRepository.existePorNombre(zona.getNombre())) {
            throw new IllegalArgumentException("Ya existe una zona con el nombre: " + zona.getNombre());
        }
        zona.setActivo(true);
        zona.setFechaCreacion(LocalDateTime.now(ZoneId.of("America/Bogota")));
        return zonaRepository.guardar(zona);
    }

    @Override
    public Zona actualizarZona(Long idZona, Zona zona, boolean confirmarReduccionCapacidad) {
        Zona existente = zonaRepository.buscarPorId(idZona)
                .orElseThrow(() -> new IllegalArgumentException("Zona no encontrada con ID: " + idZona));

        if (zonaRepository.existePorNombreYDistintoId(zona.getNombre(), idZona)) {
            throw new IllegalArgumentException("Ya existe otra zona con el nombre: " + zona.getNombre());
        }

        // HU21b: Reducir la capacidad por debajo de la ocupación actual muestra advertencia y requiere confirmación
        long lotesActivos = loteRepository.contarLotesActivosPorZona(idZona);
        if (zona.getCapacidadMaxima() != null && zona.getCapacidadMaxima() < lotesActivos && !confirmarReduccionCapacidad) {
            throw new IllegalArgumentException("ADVERTENCIA_CAPACIDAD: La nueva capacidad (" + zona.getCapacidadMaxima() +
                    ") es menor que los lotes activos actuales (" + lotesActivos + "). Requiere confirmación para proceder.");
        }

        existente.setNombre(zona.getNombre());
        existente.setCapacidadMaxima(zona.getCapacidadMaxima());
        existente.setTipoCondicion(zona.getTipoCondicion());
        existente.setExposicionSolar(zona.getExposicionSolar());

        return zonaRepository.guardar(existente);
    }

    @Override
    public Zona activarZona(Long idZona) {
        Zona existente = zonaRepository.buscarPorId(idZona)
                .orElseThrow(() -> new IllegalArgumentException("Zona no encontrada con ID: " + idZona));
        existente.setActivo(true);
        return zonaRepository.guardar(existente);
    }

    @Override
    public Zona desactivarZona(Long idZona) {
        Zona existente = zonaRepository.buscarPorId(idZona)
                .orElseThrow(() -> new IllegalArgumentException("Zona no encontrada con ID: " + idZona));
        
        long lotesActivos = loteRepository.contarLotesActivosPorZona(idZona);
        if (lotesActivos > 0) {
            throw new IllegalStateException("No se puede desactivar la zona porque tiene " + lotesActivos + " lotes activos.");
        }
        
        existente.setActivo(false);
        return zonaRepository.guardar(existente);
    }
}
