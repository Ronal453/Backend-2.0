package com.plantopolis.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resumen agregado de trazabilidad de lotes por proveedor (HU35 criterio 2 / RF34).
 * <p>
 * Permite evaluar la calidad del material vegetal de cada proveedor comparando
 * las plantas iniciales con las vivas y el número de lotes descartados.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteProveedor {
    private Long idProveedor;
    private String nombreProveedor;
    private String tipoInsumo;
    private Boolean activo;

    /** Total de lotes históricos asociados al proveedor. */
    private long totalLotes;
    /** Lotes aún en cultivo (no DESCARTADO ni EN_TIENDA). */
    private long lotesEnCultivo;
    /** Lotes vinculados al catálogo (EN_TIENDA). */
    private long lotesEnTienda;
    /** Lotes descartados (plaga, merma total, etc.). */
    private long lotesDescartados;

    /** Suma de cantidades iniciales sembradas. */
    private long plantasIniciales;
    /** Suma de cantidades vivas actuales. */
    private long plantasActuales;
    /** Porcentaje de pérdida = (iniciales - actuales) / iniciales * 100, redondeado a 2 decimales. */
    private double porcentajePerdida;
}
