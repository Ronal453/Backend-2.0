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
public class Merma {
    private Long idMerma;
    private Long idLote;
    private String codigoLote;
    private String especieLote;
    private Long idCausa;
    private String nombreCausa;
    private Long idUsuario;
    private String nombreUsuario;
    private Integer cantidadPerdida;
    private LocalDate fechaMerma;
    private String observaciones;
    private LocalDateTime fechaRegistro;
}
