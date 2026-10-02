package com.plantopolis.backend.infrastructure.adapter.out.persistence;

import com.plantopolis.backend.domain.model.ProductoMasVendido;
import com.plantopolis.backend.domain.port.out.ReporteRepositoryPort;
import com.plantopolis.backend.infrastructure.persistence.repository.DetallePedidoJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.repository.PagoJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.repository.PedidoJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ReporteJpaAdapter implements ReporteRepositoryPort {

    private final PagoJpaRepository pagoRepo;
    private final PedidoJpaRepository pedidoRepo;
    private final DetallePedidoJpaRepository detalleRepo;
    private final EntityManager entityManager;

    @Override
    public BigDecimal obtenerTotalIngresosAprobados(java.time.LocalDateTime fechaInicio, java.time.LocalDateTime fechaFin) {
        return pagoRepo.getTotalIngresosAprobados(fechaInicio, fechaFin);
    }

    @Override
    public long contarTotalPedidos(java.time.LocalDateTime fechaInicio, java.time.LocalDateTime fechaFin) {
        return pedidoRepo.contarTotalPedidos(fechaInicio, fechaFin);
    }

    @Override
    public Map<String, Long> contarPedidosPorEstado(java.time.LocalDateTime fechaInicio, java.time.LocalDateTime fechaFin) {
        Map<String, Long> map = new LinkedHashMap<>();
        pedidoRepo.countPorEstado(fechaInicio, fechaFin).forEach(row -> {
            String estado = (String) row[0];
            Long count = (Long) row[1];
            map.put(estado, count);
        });
        return map;
    }

    @Override
    public List<ProductoMasVendido> obtenerTopProductosVendidos(int limite, java.time.LocalDateTime fechaInicio, java.time.LocalDateTime fechaFin) {
        return detalleRepo
                .findTopProductos(fechaInicio, fechaFin, PageRequest.of(0, limite))
                .stream()
                .map(row -> ProductoMasVendido.builder()
                        .idProducto((Long) row[0])
                        .nombreProducto((String) row[1])
                        .totalVendido((Long) row[2])
                        .totalIngresos((BigDecimal) row[3])
                        .build())
                .toList();
    }

    @Override
    public Long contarTotalPlantasPerdidas() {
        Query query = entityManager.createQuery("SELECT SUM(m.cantidadPerdida) FROM MermaEntity m");
        List<?> results = query.getResultList();
        if (results == null || results.isEmpty() || results.get(0) == null) {
            return 0L;
        }
        // Hibernate SUM typically returns Long
        Object first = results.get(0);
        if (first instanceof Long) return (Long) first;
        if (first instanceof Integer) return ((Integer) first).longValue();
        if (first instanceof Object[] arr && arr.length > 0 && arr[0] instanceof Long) {
            return (Long) arr[0];
        }
        return 0L;
    }

    @Override
    public Map<String, Long> contarLotesPorFase() {
        Query query = entityManager.createQuery("SELECT l.estadoLote, COUNT(l) FROM LoteProduccionEntity l GROUP BY l.estadoLote");
        List<Object[]> results = query.getResultList();
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : results) {
            map.put((String) row[0], (Long) row[1]);
        }
        return map;
    }

    @Override
    public Map<String, Long> contarTareasPorEstado() {
        Query query = entityManager.createQuery("SELECT t.estadoTarea, COUNT(t) FROM TareaEntity t GROUP BY t.estadoTarea");
        List<Object[]> results = query.getResultList();
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : results) {
            map.put((String) row[0], (Long) row[1]);
        }
        return map;
    }

    @Override
    public Map<String, Long> sumarMermasPorCausa() {
        Query query = entityManager.createQuery("SELECT c.nombreCausa, SUM(m.cantidadPerdida) FROM MermaEntity m JOIN m.causa c GROUP BY c.nombreCausa");
        List<Object[]> results = query.getResultList();
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : results) {
            map.put((String) row[0], ((Number) row[1]).longValue());
        }
        return map;
    }
}
