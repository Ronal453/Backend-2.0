package com.plantopolis.backend.domain.model;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemCarrito {
    private Long idItem;
    private Long idCarrito;
    private Long idProducto;
    private Integer cantidad;
    // Datos enriquecidos
    private String nombreProducto;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
    private String imagenUrl;
}
