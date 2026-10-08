package com.plantopolis.backend.infrastructure.adapter.out.persistence;

import com.plantopolis.backend.domain.model.HistorialEstadoLote;
import com.plantopolis.backend.domain.model.LoteProduccion;
import com.plantopolis.backend.domain.port.out.HistorialEstadoLoteRepositoryPort;
import com.plantopolis.backend.domain.port.out.LoteRepositoryPort;
import com.plantopolis.backend.infrastructure.persistence.mapper.LoteMapper;
import com.plantopolis.backend.infrastructure.persistence.repository.HistorialEstadoLoteJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.repository.LoteJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LoteJpaAdapter implements LoteRepositoryPort, HistorialEstadoLoteRepositoryPort {

    private final LoteJpaRepository loteRepo;
    private final HistorialEstadoLoteJpaRepository historialRepo;
    private final LoteMapper mapper;

    @Override
    public Page<LoteProduccion> buscarConFiltros(Long idZona, String estadoLote, String termino, Pageable pageable) {
        return loteRepo.buscarConFiltros(idZona, estadoLote, termino, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<LoteProduccion> buscarPorId(Long idLote) {
        return loteRepo.findById(idLote).map(mapper::toDomain);
    }

    @Override
    public LoteProduccion guardar(LoteProduccion lote) {
        var entity = mapper.toEntity(lote);
        var guardado = loteRepo.save(entity);
        return loteRepo.findById(guardado.getIdLote())
                .map(mapper::toDomain)
                .orElse(mapper.toDomain(guardado));
    }

    @Override
    public HistorialEstadoLote guardar(HistorialEstadoLote historial) {
        var entity = mapper.toEntityHistorial(historial);
        var guardado = historialRepo.save(entity);
        return historialRepo.findById(guardado.getIdHistorialLote())
                .map(mapper::toDomainHistorial)
                .orElse(mapper.toDomainHistorial(guardado));
    }

    @Override
    public List<HistorialEstadoLote> buscarPorIdLote(Long idLote) {
        return historialRepo.findByIdLoteOrderByFechaCambioDesc(idLote)
                .stream()
                .map(mapper::toDomainHistorial)
                .toList();
    }

    @Override
    public long contarLotesActivosPorZona(Long idZona) {
        return loteRepo.countByIdZonaAndEstadoLoteNotIn(idZona, List.of("DESCARTADO", "EN_TIENDA"));
    }

    @Override
    public long contarPlantasActivasPorZona(Long idZona) {
        return loteRepo.sumarPlantasActivasPorZona(idZona);
    }

    @Override
    public List<LoteProduccion> buscarLotesActivosPorZona(Long idZona) {
        return loteRepo.findByIdZonaAndEstadoLoteNotInOrderByFechaSiembraDesc(idZona, List.of("DESCARTADO", "EN_TIENDA"))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    /** {@inheritDoc} */
    @Override
    public List<LoteProduccion> buscarLotesPorProveedor(Long idProveedor) {
        return loteRepo.findByIdProveedorOrderByFechaSiembraDesc(idProveedor)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    /** {@inheritDoc} */
    @Override
    public List<LoteProduccion> buscarLotesConProveedor() {
        return loteRepo.findByIdProveedorIsNotNull()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
