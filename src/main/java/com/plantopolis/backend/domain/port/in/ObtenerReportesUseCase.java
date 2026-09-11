package com.plantopolis.backend.domain.port.in;

import com.plantopolis.backend.domain.model.ReporteVentas;
import java.time.LocalDate;

public interface ObtenerReportesUseCase {

    ReporteVentas obtenerReporte();

    /**
     * Genera un CSV (bytes UTF-8 con BOM) con el detalle de pedidos dentro
     * del rango de fechas indicado. Método aditivo, no toca obtenerReporte().
     */
    byte[] exportarPedidosCsv(LocalDate fechaInicio, LocalDate fechaFin);
}