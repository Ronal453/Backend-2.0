package com.plantopolis.backend.domain.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pago {
    private Long idPago;
    private Long idPedido;
    private Long idMetodo;
    private Long idEstadoPago;
    private BigDecimal monto;
    private LocalDateTime fechaPago;
    // Enriquecidos
    private String nombreMetodo;
    private String estadoPagoDescripcion;
}