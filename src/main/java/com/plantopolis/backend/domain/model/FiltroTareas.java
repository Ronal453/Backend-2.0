package com.plantopolis.backend.domain.model;

import lombok.Builder;

/**
 * Criterios de filtrado combinables para consultas administrativas de tareas (HU24, HU29).
 */
@Builder
public record FiltroTareas(
        String estado,
        String prioridad,
        String tipo,
        Long idTrabajador,
        Boolean sinAsignar,
        Boolean soloVencidas,
        Long idZona,
        Long idLote
) {}
