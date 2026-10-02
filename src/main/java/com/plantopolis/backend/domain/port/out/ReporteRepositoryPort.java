package com.plantopolis.backend.domain.port.out;

import com.plantopolis.backend.domain.model.ProductoMasVendido;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ReporteRepositoryPort {

    BigDecimal obtenerTotalIngresosAprobados(java.time.LocalDateTime fechaInicio, java.time.LocalDateTime fechaFin);

    long contarTotalPedidos(java.time.LocalDateTime fechaInicio, java.time.LocalDateTime fechaFin);

    /**
     * Retorna un mapa con el conteo de pedidos por descripcion de estado.
     */
    Map<String, Long> contarPedidosPorEstado(java.time.LocalDateTime fechaInicio, java.time.LocalDateTime fechaFin);

    /**
     * Obtiene los productos mas vendidos con un limite de resultados.
     */
    List<ProductoMasVendido> obtenerTopProductosVendidos(int limite, java.time.LocalDateTime fechaInicio, java.time.LocalDateTime fechaFin);

    /**
     * Retorna el numero total de plantas perdidas sumando las cantidades de todas las mermas.
     */
    Long contarTotalPlantasPerdidas();

    /**
     * Retorna un mapa con el conteo de lotes por fase (GERMINANDO, CRECIENDO, etc.)
     */
    Map<String, Long> contarLotesPorFase();

    /**
     * Retorna un mapa con el conteo de tareas por estado (POR_HACER, EN_PROGRESO, etc.)
     */
    Map<String, Long> contarTareasPorEstado();

    /**
     * Retorna un mapa sumando la cantidad de plantas perdidas agrupado por el nombre de la causa.
     */
    Map<String, Long> sumarMermasPorCausa();
}
