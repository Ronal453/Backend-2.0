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
public class LoteProduccion {
    private Long idLote;
    private String codigoLote;
    private String especie;
    private Integer cantidadInicial;
    private Integer cantidadActual;
    private LocalDate fechaSiembra;
    private String estadoLote;
    private Long idZona;
    private String nombreZona;
    private Long idProveedor;
    private String nombreProveedor;
    private Long idProducto;
    private String nombreProducto;
    private Boolean esVinculado;
    private LocalDateTime fechaVinculacion;
    private LocalDateTime fechaCreacion;
}
