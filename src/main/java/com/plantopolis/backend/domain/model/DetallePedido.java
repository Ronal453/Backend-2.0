package com.plantopolis.backend.domain.model;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetallePedido {
    private Long idDetalle;
    private Long idPedido;
    private Long idProducto;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    // Enriquecidos
    private String nombreProducto;
    private String imagenUrl;
    private BigDecimal subtotal;
}