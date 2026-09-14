package com.plantopolis.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ZONA")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZonaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_ZONA")
    private Long idZona;

    @Column(name = "NOMBRE", nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(name = "CAPACIDAD_MAXIMA", nullable = false)
    private Integer capacidadMaxima;

    @Column(name = "TIPO_CONDICION", nullable = false, length = 50)
    private String tipoCondicion;

    @Column(name = "EXPOSICION_SOLAR", nullable = false, length = 50)
    private String exposicionSolar;

    @Column(name = "ACTIVO", nullable = false)
    private Boolean activo;

    @Column(name = "FECHA_CREACION", nullable = false)
    private LocalDateTime fechaCreacion;
}
