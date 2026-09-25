package com.plantopolis.backend.domain.port.out;

import com.plantopolis.backend.domain.model.ProductoMasVendido;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ReporteRepositoryPort {

    BigDecimal obtenerTotalIngresosAprobados();

    long contarTotalPedidos();

    /**
     * Retorna un mapa con el conteo de pedidos por descripción de estado.
     */
    Map<String, Long> contarPedidosPorEstado();

    /**
     * Obtiene los productos más vendidos con un límite de resultados.
     */
    List<ProductoMasVendido> obtenerTopProductosVendidos(int limite);
}
