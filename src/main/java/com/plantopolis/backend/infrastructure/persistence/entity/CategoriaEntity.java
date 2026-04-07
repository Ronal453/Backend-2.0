package com.plantopolis.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "CATEGORIA")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_CATEGORIA")
    private Long idCategoria;

    @Column(name = "NOMBRE_CATEGORIA", nullable = false, length = 100)
    private String nombreCategoria;

    @Lob                          // ← esto soluciona el error
    @Column(name = "DESCRIPCION")
    private String descripcion;
}
