package com.plantopolis.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Modelo de dominio que representa la definición de una tarea periódica o recurrente (HU23).
 * A partir de esta regla, el proceso en segundo plano genera instancias independientes en la tabla TAREA.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TareaRecurrente {
    private Long idTareaRecurrente;
    private String tipoTarea;
    private String prioridad;
    private String descripcion;
    private Integer frecuenciaDias;
    private Long idZona;
    private String nombreZona;
    private Long idLote;
    private String codigoLote;
    private Long idTrabajadorAsignado;
    private String nombreTrabajador;
    private String emailTrabajador;
    private Boolean activo;
    private LocalDate fechaProximaEjecucion;
    private LocalDateTime fechaCreacion;
}
