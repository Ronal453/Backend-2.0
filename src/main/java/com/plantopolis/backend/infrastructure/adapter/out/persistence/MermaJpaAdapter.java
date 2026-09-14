package com.plantopolis.backend.infrastructure.adapter.out.persistence;

import com.plantopolis.backend.domain.model.CausaMerma;
import com.plantopolis.backend.domain.model.Merma;
import com.plantopolis.backend.domain.port.out.CausaMermaRepositoryPort;
import com.plantopolis.backend.domain.port.out.MermaRepositoryPort;
import com.plantopolis.backend.infrastructure.persistence.mapper.MermaMapper;
import com.plantopolis.backend.infrastructure.persistence.repository.CausaMermaJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.repository.MermaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MermaJpaAdapter implements MermaRepositoryPort, CausaMermaRepositoryPort {

    private final MermaJpaRepository mermaRepo;
    private final CausaMermaJpaRepository causaRepo;
    private final MermaMapper mapper;

    @Override
    public List<CausaMerma> listarActivas() {
        return causaRepo.findByActivoTrueOrderByNombreCausaAsc()
                .stream()
                .map(mapper::toDomainCausa)
                .toList();
    }

    @Override
    public Optional<CausaMerma> buscarPorId(Long idCausa) {
        return causaRepo.findById(idCausa).map(mapper::toDomainCausa);
    }

    @Override
    public Merma guardar(Merma merma) {
        var entity = mapper.toEntity(merma);
        var guardada = mermaRepo.save(entity);
        return mermaRepo.findById(guardada.getIdMerma())
                .map(mapper::toDomain)
                .orElse(mapper.toDomain(guardada));
    }

    @Override
    public List<Merma> listarRecientes(int limite) {
        return mermaRepo.listarRecientes(PageRequest.of(0, limite))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
