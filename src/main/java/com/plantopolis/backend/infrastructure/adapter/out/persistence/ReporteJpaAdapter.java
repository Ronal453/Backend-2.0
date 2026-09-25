package com.plantopolis.backend.infrastructure.adapter.out.persistence;

import com.plantopolis.backend.domain.model.ProductoMasVendido;
import com.plantopolis.backend.domain.port.out.ReporteRepositoryPort;
import com.plantopolis.backend.infrastructure.persistence.repository.DetallePedidoJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.repository.PagoJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.repository.PedidoJpaRepository;
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

    @Override
    public BigDecimal obtenerTotalIngresosAprobados() {
        return pagoRepo.getTotalIngresosAprobados();
    }

    @Override
    public long contarTotalPedidos() {
        return pedidoRepo.count();
    }

    @Override
    public Map<String, Long> contarPedidosPorEstado() {
        Map<String, Long> map = new LinkedHashMap<>();
        pedidoRepo.countPorEstado().forEach(row -> {
            String estado = (String) row[0];
            Long count = (Long) row[1];
            map.put(estado, count);
        });
        return map;
    }

    @Override
    public List<ProductoMasVendido> obtenerTopProductosVendidos(int limite) {
        return detalleRepo
                .findTopProductos(PageRequest.of(0, limite))
                .stream()
                .map(row -> ProductoMasVendido.builder()
                        .idProducto((Long) row[0])
                        .nombreProducto((String) row[1])
                        .totalVendido((Long) row[2])
                        .totalIngresos((BigDecimal) row[3])
                        .build())
                .toList();
    }
}
