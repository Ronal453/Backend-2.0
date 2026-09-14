package com.plantopolis.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialTarea {
    private Long idHistorialTarea;
    private Long idTarea;
    private Long idUsuario;
    private String nombreUsuario;
    private String tipoCambio;
    private String estadoAnterior;
    private String estadoNuevo;
    private Long idTrabajadorAnterior;
    private String nombreTrabajadorAnterior;
    private Long idTrabajadorNuevo;
    private String nombreTrabajadorNuevo;
    private LocalDateTime fechaCambio;
}
