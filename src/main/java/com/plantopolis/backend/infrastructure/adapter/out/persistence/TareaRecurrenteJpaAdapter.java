package com.plantopolis.backend.infrastructure.adapter.out.persistence;

import com.plantopolis.backend.domain.model.TareaRecurrente;
import com.plantopolis.backend.domain.port.out.TareaRecurrenteRepositoryPort;
import com.plantopolis.backend.infrastructure.persistence.mapper.TareaMapper;
import com.plantopolis.backend.infrastructure.persistence.repository.TareaRecurrenteJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador JPA que implementa el puerto secundario de TareaRecurrenteRepositoryPort (HU23).
 */
@Component
@RequiredArgsConstructor
public class TareaRecurrenteJpaAdapter implements TareaRecurrenteRepositoryPort {

    private final TareaRecurrenteJpaRepository recurrenteRepo;
    private final TareaMapper mapper;

    @Override
    public TareaRecurrente guardarRecurrente(TareaRecurrente recurrente) {
        var entity = mapper.toEntityRecurrente(recurrente);
        var guardada = recurrenteRepo.save(entity);
        return recurrenteRepo.findById(guardada.getIdTareaRecurrente())
                .map(mapper::toDomainRecurrente)
                .orElse(mapper.toDomainRecurrente(guardada));
    }

    @Override
    public Optional<TareaRecurrente> buscarRecurrentePorId(Long id) {
        return recurrenteRepo.findById(id).map(mapper::toDomainRecurrente);
    }

    @Override
    public List<TareaRecurrente> listarRecurrentes() {
        return recurrenteRepo.findAllByOrderByIdTareaRecurrenteDesc()
                .stream()
                .map(mapper::toDomainRecurrente)
                .toList();
    }

    @Override
    public List<TareaRecurrente> buscarRecurrentesActivasPendientes(LocalDate hoy) {
        return recurrenteRepo.buscarActivasPendientes(hoy)
                .stream()
                .map(mapper::toDomainRecurrente)
                .toList();
    }
}
