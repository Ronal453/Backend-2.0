package com.plantopolis.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad JPA para la tabla CATEGORIA.
 */
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

    /**
     * Descripción extensa de la categoría botánica.
     * Se define como TEXT en lugar de @Lob para compatibilidad nativa con PostgreSQL sin usar OIDs.
     */
    @Column(name = "DESCRIPCION", columnDefinition = "TEXT")
    private String descripcion;
}
