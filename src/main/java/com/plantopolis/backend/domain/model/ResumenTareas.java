package com.plantopolis.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Agregado de métricas operativas de tareas para el Dashboard Operativo del Administrador (HU29).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenTareas {
    private long totalActivas;
    private long vencidasActivas;
    private long completadasEnPeriodo;
    private long sinAsignar;

    private Map<String, Long> conteoPorEstado;
    private List<CargaTrabajador> cargaPorTrabajador;
    private List<PuntoSerieCompletadas> completadasPorDia;
    private List<Tarea> tareasVencidas;

    /**
     * Resumen de carga operativa de un trabajador específico.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CargaTrabajador {
        private Long idTrabajador;
        private String nombreTrabajador;
        private String emailTrabajador;
        private long porHacer;
        private long enProgreso;
        private long enRevision;
        private long bloqueadas;
        private long completadasPeriodo;
        private long vencidas;
    }

    /**
     * Punto temporal para la gráfica de evolución de tareas completadas.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PuntoSerieCompletadas {
        private LocalDate fecha;
        private long cantidad;
    }
}
