package com.plantopolis.backend.domain.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Carrito {
    private Long idCarrito;
    private Long idUsuario;
    private Long idEstadoCarrito;
    private String estadoDescripcion;
    private LocalDateTime fechaCreacion;
    private List<ItemCarrito> items;
    // Total calculado
    private BigDecimal total;
}
