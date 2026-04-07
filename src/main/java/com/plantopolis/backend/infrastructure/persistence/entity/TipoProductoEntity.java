package com.plantopolis.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TIPOPRODUCTO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipoProductoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_TIPO")
    private Long idTipo;

    @Column(name = "NOMBRE_TIPO", nullable = false, length = 80)
    private String nombreTipo;
}
