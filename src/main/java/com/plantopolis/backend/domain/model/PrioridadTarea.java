package com.plantopolis.backend.domain.model;

/**
 * Niveles de prioridad aplicables a una tarea operativa de vivero (HU22).
 */
public enum PrioridadTarea {
    ALTA,
    MEDIA,
    BAJA;

    /**
     * Valida y normaliza un nivel de prioridad desde texto.
     *
     * @param texto Cadena de texto con la prioridad.
     * @return Constante de {@link PrioridadTarea} correspondiente.
     * @throws IllegalArgumentException Si la prioridad no es ALTA, MEDIA o BAJA.
     */
    public static PrioridadTarea desde(String texto) {
        if (texto == null || texto.isBlank()) {
            return MEDIA; // Prioridad por defecto según RF-22 / MER
        }
        try {
            return PrioridadTarea.valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Prioridad no válida: '" + texto + "'. Permitidos: ALTA, MEDIA, BAJA");
        }
    }
}
