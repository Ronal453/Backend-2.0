package com.plantopolis.backend.domain.port.in;

import com.plantopolis.backend.domain.model.ReporteVentas;

/**
 * Caso de uso: obtener reportes y métricas de ventas para el dashboard admin.
 *
 * HU10 — Prioridad: Media — Sprint 5
 *
 * Provee un snapshot de:
 *   - Ingresos totales (pagos aprobados)
 *   - Total de pedidos y productos activos
 *   - Distribución de pedidos por estado
 *   - Top 5 productos más vendidos por unidades
 *
 * 
 */
public interface ObtenerReportesUseCase {

    /**
     * Calcula y devuelve todas las métricas de ventas del sistema.
     * Las métricas se calculan en tiempo real desde la BD.
     *
     * @return objeto ReporteVentas con todas las métricas agregadas
     */
    ReporteVentas obtenerReporte();
}