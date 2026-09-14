package com.plantopolis.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "CAUSAMERMA")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CausaMermaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_CAUSA")
    private Long idCausa;

    @Column(name = "NOMBRE_CAUSA", nullable = false, unique = true, length = 80)
    private String nombreCausa;

    @Column(name = "ACTIVO", nullable = false)
    private Boolean activo;
}
