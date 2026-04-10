package com.plantopolis.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ESTADOPEDIDO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadoPedidoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_ESTADO")
    private Long idEstado;

    @Column(name = "DESCRIPCION_ESTADO", nullable = false, length = 80)
    private String descripcionEstado;
}