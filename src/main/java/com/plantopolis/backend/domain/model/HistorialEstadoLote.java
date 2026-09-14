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
public class HistorialEstadoLote {
    private Long idHistorialLote;
    private Long idLote;
    private Long idUsuario;
    private String nombreUsuario;
    private String estadoAnterior;
    private String estadoNuevo;
    private String observaciones;
    private LocalDateTime fechaCambio;
}
