package com.plantopolis.backend.infrastructure.adapter.out.persistence;

import com.plantopolis.backend.domain.model.ComentarioTarea;
import com.plantopolis.backend.domain.model.FiltroTareas;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador JPA que implementa los puertos de persistencia de Tarea, Historial y Comentarios.
 */
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
    public List<Tarea> buscarConFiltrosAdmin(FiltroTareas filtro, LocalDate hoy) {
        String estado = (filtro != null && filtro.estado() != null && !filtro.estado().isBlank()) ? filtro.estado().trim() : null;
        String prioridad = (filtro != null && filtro.prioridad() != null && !filtro.prioridad().isBlank()) ? filtro.prioridad().trim() : null;
        String tipo = (filtro != null && filtro.tipo() != null && !filtro.tipo().isBlank()) ? filtro.tipo().trim() : null;
        Long idTrabajador = filtro != null ? filtro.idTrabajador() : null;
        Boolean sinAsignar = filtro != null ? filtro.sinAsignar() : null;
        Boolean soloVencidas = filtro != null ? filtro.soloVencidas() : null;
        Long idZona = filtro != null ? filtro.idZona() : null;
        Long idLote = filtro != null ? filtro.idLote() : null;

        return tareaRepo.buscarConFiltrosAdmin(estado, prioridad, tipo, idTrabajador, sinAsignar, soloVencidas, hoy, idZona, idLote)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Tarea> buscarVencidasSinAlerta(LocalDate hoy) {
        return tareaRepo.buscarVencidasSinAlerta(hoy)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existeInstanciaRecurrente(Long idTareaRecurrente, LocalDate fechaLimite) {
        return tareaRepo.existsByIdTareaRecurrenteAndFechaLimite(idTareaRecurrente, fechaLimite);
    }

    @Override
    @Transactional
    public void marcarAlertaVencidaEnviada(List<Long> idsTareas) {
        if (idsTareas != null && !idsTareas.isEmpty()) {
            tareaRepo.marcarAlertaVencidaEnviada(idsTareas);
        }
    }

    @Override
    public List<Tarea> buscarTodasParaResumen() {
        return tareaRepo.buscarTodasParaResumen()
                .stream()
                .map(mapper::toDomain)
                .toList();
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
