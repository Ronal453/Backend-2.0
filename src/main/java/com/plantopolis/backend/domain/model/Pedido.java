package com.plantopolis.backend.domain.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pedido {
    private Long idPedido;
    private Long idUsuario;
    private Long idEstado;
    private Long idCarrito;
    private LocalDateTime fechaPedido;
    private String direccionEnvio;
    private String numeroPedido;
    // Enriquecidos
    private String estadoDescripcion;
    private String emailCliente;
    private String nombreCliente;
    private List<DetallePedido> detalles;
    private Pago pago;
    private BigDecimal total;
}