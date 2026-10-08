package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import com.plantopolis.backend.domain.model.ReporteProveedor;

/**
 * Fila del reporte de lotes por proveedor (HU35 criterio 2).
 */
public record ReporteProveedorResponse(
        Long idProveedor,
        String nombreProveedor,
        String tipoInsumo,
        Boolean activo,
        long totalLotes,
        long lotesEnCultivo,
        long lotesEnTienda,
        long lotesDescartados,
        long plantasIniciales,
        long plantasActuales,
        double porcentajePerdida
) {
    /**
     * @param r modelo agregado de dominio
     * @return DTO equivalente
     */
    public static ReporteProveedorResponse from(ReporteProveedor r) {
        return new ReporteProveedorResponse(
                r.getIdProveedor(),
                r.getNombreProveedor(),
                r.getTipoInsumo(),
                r.getActivo(),
                r.getTotalLotes(),
                r.getLotesEnCultivo(),
                r.getLotesEnTienda(),
                r.getLotesDescartados(),
                r.getPlantasIniciales(),
                r.getPlantasActuales(),
                r.getPorcentajePerdida()
        );
    }
}
