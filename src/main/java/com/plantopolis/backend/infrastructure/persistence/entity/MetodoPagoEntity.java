package com.plantopolis.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "METODOPAGO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetodoPagoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_METODO")
    private Long idMetodo;

    @Column(name = "NOMBRE_METODO", nullable = false, length = 80)
    private String nombreMetodo;
}