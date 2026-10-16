package com.plantopolis.backend.domain.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Representa los estados del ciclo de vida de una tarea operativa de vivero
 * y define de manera estricta las transiciones válidas según las reglas de negocio
 * de las Historias de Usuario HU24 y HU25.
 *
 * <p>Flujo base: {@code POR_HACER -> EN_PROGRESO -> EN_REVISION -> COMPLETADA}.</p>
 * <p>{@code COMPLETADA} es un estado terminal sin transiciones de salida.</p>
 * <p>{@code BLOQUEADA} puede alcanzarse desde cualquier estado activo (excepto COMPLETADA)
 * y únicamente puede regresar a {@code POR_HACER} o {@code EN_PROGRESO}.</p>
 */
public enum EstadoTarea {
    POR_HACER,
    EN_PROGRESO,
    EN_REVISION,
    COMPLETADA,
    BLOQUEADA;

    /**
     * Obtiene el conjunto de estados válidos a los que se puede transicionar desde este estado.
     *
     * @return Conjunto inmutable o protegido de estados permitidos como destino.
     */
    public Set<EstadoTarea> destinosPermitidos() {
        return switch (this) {
            case POR_HACER -> EnumSet.of(EN_PROGRESO, BLOQUEADA);
            case EN_PROGRESO -> EnumSet.of(EN_REVISION, POR_HACER, BLOQUEADA);
            case EN_REVISION -> EnumSet.of(COMPLETADA, EN_PROGRESO, BLOQUEADA);
            case BLOQUEADA -> EnumSet.of(POR_HACER, EN_PROGRESO);
            case COMPLETADA -> EnumSet.noneOf(EstadoTarea.class);
        };
    }

    /**
     * Valida si la transición hacia el estado destino está autorizada por la máquina de estados.
     *
     * @param destino Estado al que se pretende mover la tarea.
     * @return {@code true} si la transición es válida; {@code false} en caso contrario.
     */
    public boolean puedeTransicionarA(EstadoTarea destino) {
        if (destino == null) {
            return false;
        }
        return destinosPermitidos().contains(destino);
    }

    /**
     * Convierte y normaliza una cadena de texto en un valor de {@link EstadoTarea}.
     *
     * @param texto Cadena de texto representativa (insensible a mayúsculas/espacios).
     * @return Constante de {@link EstadoTarea} correspondiente.
     * @throws IllegalArgumentException Si el texto es nulo, vacío o no coincide con ningún estado válido.
     */
    public static EstadoTarea desde(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("El estado de la tarea no puede estar vacío");
        }
        try {
            return EstadoTarea.valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado de tarea no válido: '" + texto + "'. Permitidos: " +
                    "POR_HACER, EN_PROGRESO, EN_REVISION, COMPLETADA, BLOQUEADA");
        }
    }
}
