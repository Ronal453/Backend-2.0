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
public class Zona {
    private Long idZona;
    private String nombre;
    private Integer capacidadMaxima;
    private String tipoCondicion;
    private String exposicionSolar;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
}
