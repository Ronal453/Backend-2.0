package com.plantopolis.backend.domain.model;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Producto {
    private Long idProducto;
    private Long idCategoria;
    private Long idTipo;
    private String nombreProducto;
    private String descripcion;
    private BigDecimal precio;

    private BigDecimal porcentajeIva;

    private Integer stock;

    private Integer stockMinimoAlerta;

    private String imagenUrl;
    private String cuidados;
    private String luz;
    private String riego;
    private String tamanioEstimado;
    private Boolean activo;
    // Datos enriquecidos para la respuesta
    private String nombreCategoria;
    private String nombreTipo;
}