package com.plantopolis.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "PAGO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_PAGO")
    private Long idPago;

    @Column(name = "ID_PEDIDO", nullable = false, unique = true)
    private Long idPedido;

    @Column(name = "ID_METODO", nullable = false)
    private Long idMetodo;

    @Column(name = "ID_ESTADO_PAGO", nullable = false)
    private Long idEstadoPago;

    @Column(name = "MONTO", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "FECHA_PAGO")
    private LocalDateTime fechaPago;

    // Relaciones de solo lectura
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_PEDIDO", insertable = false, updatable = false)
    private PedidoEntity pedido;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ID_METODO", insertable = false, updatable = false)
    private MetodoPagoEntity metodo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ID_ESTADO_PAGO", insertable = false, updatable = false)
    private EstadoPagoEntity estadoPago;
}