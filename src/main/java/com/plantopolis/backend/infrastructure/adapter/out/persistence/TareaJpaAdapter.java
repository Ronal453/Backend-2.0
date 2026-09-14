package com.plantopolis.backend.infrastructure.adapter.out.persistence;

import com.plantopolis.backend.domain.model.ComentarioTarea;
import com.plantopolis.backend.domain.model.HistorialTarea;
import com.plantopolis.backend.domain.model.Tarea;
import com.plantopolis.backend.domain.port.out.ComentarioTareaRepositoryPort;
import com.plantopolis.backend.domain.port.out.HistorialTareaRepositoryPort;
import com.plantopolis.backend.domain.port.out.TareaRepositoryPort;
import com.plantopolis.backend.infrastructure.persistence.mapper.TareaMapper;
import com.plantopolis.backend.infrastructure.persistence.repository.ComentarioTareaJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.repository.HistorialTareaJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.repository.TareaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TareaJpaAdapter implements TareaRepositoryPort, HistorialTareaRepositoryPort, ComentarioTareaRepositoryPort {

    private final TareaJpaRepository tareaRepo;
    private final HistorialTareaJpaRepository historialRepo;
    private final ComentarioTareaJpaRepository comentarioRepo;
    private final TareaMapper mapper;

    @Override
    public List<Tarea> buscarPorTrabajador(Long idTrabajador, String estado, String prioridad) {
        return tareaRepo.buscarConFiltros(idTrabajador, estado, prioridad)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Tarea> buscarTodas(String estado, String prioridad) {
        return tareaRepo.buscarConFiltros(null, estado, prioridad)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Tarea> buscarPorId(Long idTarea) {
        return tareaRepo.findById(idTarea).map(mapper::toDomain);
    }

    @Override
    public Tarea guardar(Tarea tarea) {
        var entity = mapper.toEntity(tarea);
        var guardada = tareaRepo.save(entity);
        return tareaRepo.findById(guardada.getIdTarea())
                .map(mapper::toDomain)
                .orElse(mapper.toDomain(guardada));
    }

    @Override
    public HistorialTarea guardar(HistorialTarea historial) {
        var entity = mapper.toEntityHistorial(historial);
        var guardado = historialRepo.save(entity);
        return historialRepo.findById(guardado.getIdHistorialTarea())
                .map(mapper::toDomainHistorial)
                .orElse(mapper.toDomainHistorial(guardado));
    }

    @Override
    public List<HistorialTarea> buscarPorIdTarea(Long idTarea) {
        return historialRepo.findByIdTareaOrderByFechaCambioAsc(idTarea)
                .stream()
                .map(mapper::toDomainHistorial)
                .toList();
    }

    @Override
    public ComentarioTarea guardar(ComentarioTarea comentario) {
        var entity = mapper.toEntityComentario(comentario);
        var guardado = comentarioRepo.save(entity);
        return comentarioRepo.findById(guardado.getIdComentario())
                .map(mapper::toDomainComentario)
                .orElse(mapper.toDomainComentario(guardado));
    }

    @Override
    public List<ComentarioTarea> buscarComentariosPorIdTarea(Long idTarea) {
        return comentarioRepo.findByIdTareaOrderByFechaCreacionAsc(idTarea)
                .stream()
                .map(mapper::toDomainComentario)
                .toList();
    }
}
