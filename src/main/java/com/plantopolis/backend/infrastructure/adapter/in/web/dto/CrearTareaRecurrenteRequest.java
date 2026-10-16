package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * Petición para registrar una nueva definición de tarea recurrente periódica (HU23).
 */
public record CrearTareaRecurrenteRequest(
        @NotBlank(message = "El tipo de tarea es obligatorio (ej. RIEGO, PODA, FERTILIZACION, OTRO)")
        String tipoTarea,

        @NotBlank(message = "La prioridad es obligatoria (ALTA, MEDIA, BAJA)")
        String prioridad,

        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
        String descripcion,

        @NotNull(message = "La frecuencia en días es obligatoria")
        @Min(value = 1, message = "La frecuencia mínima es de 1 día")
        @Max(value = 365, message = "La frecuencia máxima permitida es de 365 días")
        Integer frecuenciaDias,

        Long idZona,

        Long idLote,

        Long idTrabajadorAsignado,

        @FutureOrPresent(message = "La fecha de inicio debe ser hoy o una fecha futura")
        LocalDate fechaInicio
) {}
