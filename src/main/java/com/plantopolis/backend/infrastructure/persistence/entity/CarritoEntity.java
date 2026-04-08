package com.plantopolis.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "CARRITO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarritoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_CARRITO")
    private Long idCarrito;

    @Column(name = "ID_USUARIO", nullable = false)
    private Long idUsuario;

    @Column(name = "ID_ESTADO_CARRITO", nullable = false)
    private Long idEstadoCarrito;

    @Column(name = "FECHA_CREACION")
    private LocalDateTime fechaCreacion;

    // Relación con items
    @OneToMany(mappedBy = "carrito", fetch = FetchType.EAGER,
               cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CarritoItemEntity> items;

    // Relación con estado
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ID_ESTADO_CARRITO",
                insertable = false, updatable = false)
    private EstadoCarritoEntity estado;
}