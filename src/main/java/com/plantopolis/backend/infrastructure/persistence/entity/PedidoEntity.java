package com.plantopolis.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "PEDIDO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_PEDIDO")
    private Long idPedido;

    @Column(name = "ID_USUARIO", nullable = false)
    private Long idUsuario;

    @Column(name = "ID_ESTADO_PEDIDO", nullable = false)
    private Long idEstado;


    @Column(name = "NUMERO_PEDIDO", nullable = false, unique = true, length = 30)
    private String numeroPedido;

    // subtotal antes de impuestos.
    @Column(name = "SUBTOTAL", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    // impuestos calculados (IVA) discriminados del subtotal.
    @Column(name = "IMPUESTOS", nullable = false, precision = 10, scale = 2)
    private BigDecimal impuestos;

    // total = subtotal + impuestos.
    @Column(name = "TOTAL", nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "DIRECCION_ENVIO", nullable = false, length = 255)
    private String direccionEnvio;

    @Column(name = "FECHA_PEDIDO")
    private LocalDateTime fechaPedido;

    // ── Relaciones de solo lectura ───────────────────────────
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ID_ESTADO_PEDIDO", insertable = false, updatable = false)
    private EstadoPedidoEntity estado;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ID_USUARIO", insertable = false, updatable = false)
    private UsuarioEntity usuario;

    // ── Relaciones con cascada ─────────────────────────────────
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "ID_PEDIDO")
    @Builder.Default
    private List<DetallePedidoEntity> detalles = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "ID_PEDIDO",
                referencedColumnName = "ID_PEDIDO",
                insertable = false, updatable = false)
    private PagoEntity pago;
}