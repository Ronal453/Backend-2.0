package com.plantopolis.backend.domain.port.out;

import com.plantopolis.backend.domain.model.LoteProduccion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface LoteRepositoryPort {
    Page<LoteProduccion> buscarConFiltros(Long idZona, String estadoLote, String termino, Pageable pageable);
    Optional<LoteProduccion> buscarPorId(Long idLote);
    LoteProduccion guardar(LoteProduccion lote);
    long contarLotesActivosPorZona(Long idZona);
    long contarPlantasActivasPorZona(Long idZona);
    List<LoteProduccion> buscarLotesActivosPorZona(Long idZona);

    /** HU35: todos los lotes (históricos) asociados a un proveedor, más recientes primero. */
    List<LoteProduccion> buscarLotesPorProveedor(Long idProveedor);

    /** HU35: todos los lotes que tienen proveedor asignado; base del reporte agregado. */
    List<LoteProduccion> buscarLotesConProveedor();
}
