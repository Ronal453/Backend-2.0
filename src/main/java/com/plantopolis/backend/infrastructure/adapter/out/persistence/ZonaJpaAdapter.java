package com.plantopolis.backend.infrastructure.adapter.out.persistence;

import com.plantopolis.backend.domain.model.Zona;
import com.plantopolis.backend.domain.port.out.ZonaRepositoryPort;
import com.plantopolis.backend.infrastructure.persistence.mapper.ZonaMapper;
import com.plantopolis.backend.infrastructure.persistence.repository.ZonaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ZonaJpaAdapter implements ZonaRepositoryPort {

    private final ZonaJpaRepository zonaRepo;
    private final ZonaMapper mapper;

    @Override
    public List<Zona> listarActivas() {
        return zonaRepo.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Zona> buscarPorId(Long idZona) {
        return zonaRepo.findById(idZona).map(mapper::toDomain);
    }

    @Override
    public List<Zona> listarTodas() {
        return zonaRepo.findAllByOrderByNombreAsc()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Zona guardar(Zona zona) {
        return mapper.toDomain(zonaRepo.save(mapper.toEntity(zona)));
    }

    @Override
    public boolean existePorNombre(String nombre) {
        return zonaRepo.existsByNombreIgnoreCase(nombre);
    }

    @Override
    public boolean existePorNombreYDistintoId(String nombre, Long idZona) {
        return zonaRepo.existsByNombreIgnoreCaseAndIdZonaNot(nombre, idZona);
    }
}
