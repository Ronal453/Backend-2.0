package com.plantopolis.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
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

    @Column(name = "ID_ESTADO", nullable = false)
    private Long idEstado;

    @Column(name = "ID_CARRITO")
    private Long idCarrito;

    @Column(name = "FECHA_PEDIDO")
    private LocalDateTime fechaPedido;

    @Column(name = "DIRECCION_ENVIO", length = 255)
    private String direccionEnvio;

    @Column(name = "NUMERO_PEDIDO", nullable = false, unique = true, length = 20)
    private String numeroPedido;

    // ── Relaciones de solo lectura ───────────────────────────
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ID_ESTADO", insertable = false, updatable = false)
    private EstadoPedidoEntity estado;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ID_USUARIO", insertable = false, updatable = false)
    private UsuarioEntity usuario;

    // ── Relaciones con cascada (se persisten junto al pedido) ─
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "ID_PEDIDO")   // FK en DETALLEPEDIDO
    @Builder.Default
    private List<DetallePedidoEntity> detalles = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "ID_PEDIDO",   // FK en PAGO
                referencedColumnName = "ID_PEDIDO",
                insertable = false, updatable = false)
    private PagoEntity pago;
}