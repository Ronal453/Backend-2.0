package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Petición de creación manual de una tarea de vivero por parte del administrador (HU22).
 */
public record CrearTareaRequest(
        @NotBlank(message = "El tipo de tarea es obligatorio (ej. RIEGO, PODA, FERTILIZACION, OTRO)")
        String tipoTarea,

        @NotBlank(message = "La prioridad es obligatoria (ALTA, MEDIA, BAJA)")
        String prioridad,

        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
        String descripcion,

        Long idZona,

        Long idLote,

        Long idTrabajadorAsignado,

        @NotNull(message = "La fecha límite es obligatoria")
        @FutureOrPresent(message = "La fecha límite debe ser igual o posterior a la fecha actual")
        LocalDate fechaLimite
) {}
