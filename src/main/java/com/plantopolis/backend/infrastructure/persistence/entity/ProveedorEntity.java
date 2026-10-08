package com.plantopolis.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad JPA mapeada a la tabla PROVEEDOR (módulo 3 del esquema Oracle).
 * <p>
 * El campo {@code activo} es NUMBER(1) en Oracle; Hibernate lo convierte a Boolean (0/1)
 * igual que en {@link ZonaEntity}.
 * </p>
 */
@Entity
@Table(name = "PROVEEDOR")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProveedorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_PROVEEDOR")
    private Long idProveedor;

    @Column(name = "NOMBRE", nullable = false, length = 150)
    private String nombre;

    @Column(name = "CONTACTO", length = 100)
    private String contacto;

    @Column(name = "TELEFONO", length = 20)
    private String telefono;

    @Column(name = "CORREO", length = 150)
    private String correo;

    @Column(name = "TIPO_INSUMO", nullable = false, length = 100)
    private String tipoInsumo;

    @Column(name = "ACTIVO", nullable = false)
    private Boolean activo;

    @Column(name = "FECHA_CREACION", nullable = false)
    private LocalDateTime fechaCreacion;
}
