package com.plantopolis.backend.domain.model;

/**
 * Catálogo de tipos de tareas operativas de cuidado y mantenimiento en vivero (HU22).
 */
public enum TipoTarea {
    RIEGO,
    PODA,
    FERTILIZACION,
    COSECHA,
    MONITOREO_PLAGAS,
    TRASPLANTE,
    MANTENIMIENTO,
    OTRO;

    /**
     * Valida y normaliza un tipo de tarea desde texto.
     *
     * @param texto Cadena de texto con el tipo de tarea.
     * @return Constante de {@link TipoTarea} correspondiente.
     * @throws IllegalArgumentException Si el tipo no coincide con los definidos en el sistema.
     */
    public static TipoTarea desde(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("El tipo de tarea es obligatorio");
        }
        try {
            return TipoTarea.valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de tarea no válido: '" + texto + "'. Permitidos: RIEGO, PODA, FERTILIZACION, COSECHA, MONITOREO_PLAGAS, TRASPLANTE, MANTENIMIENTO, OTRO");
        }
    }
}
