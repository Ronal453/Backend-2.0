package com.plantopolis.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tarea {
    private Long idTarea;
    private Long idTareaRecurrente;
    private String tipoTarea;
    private String prioridad;
    private String estadoTarea;
    private Long idZona;
    private String nombreZona;
    private Long idLote;
    private String codigoLote;
    private String especieLote;
    private Long idTrabajadorAsignado;
    private String nombreTrabajador;
    private String emailTrabajador;
    private LocalDate fechaLimite;
    private Boolean alertaVencidaEnviada;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaCompletada;
}
