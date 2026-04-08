package com.plantopolis.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ESTADOCARRITO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadoCarritoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_ESTADO_CARRITO")
    private Long idEstadoCarrito;

    @Column(name = "DESCRIPCION", nullable = false, length = 50)
    private String descripcion;
}
