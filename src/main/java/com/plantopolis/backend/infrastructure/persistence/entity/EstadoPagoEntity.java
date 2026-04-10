package com.plantopolis.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ESTADOPAGO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadoPagoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_ESTADO_PAGO")
    private Long idEstadoPago;

    @Column(name = "DESCRIPCION_ESTADO_PAGO", nullable = false, length = 50)
    private String descripcionEstadoPago;
}