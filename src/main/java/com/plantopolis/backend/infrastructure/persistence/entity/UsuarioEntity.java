package com.plantopolis.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "USUARIO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_USUARIO")
    private Long idUsuario;

    @Column(name = "ID_ROL", nullable = false)
    private Long idRol;

    @Column(name = "NOMBRE_COMPLETO", nullable = false, length = 150)
    private String nombreCompleto;

    @Column(name = "CORREO", nullable = false, unique = true, length = 150)
    private String correo;

    @Column(name = "CONTRASENA_HASH", nullable = false, length = 255)
    private String contrasenaHash;

    @Column(name = "TELEFONO", length = 20)
    private String telefono;

    @Column(name = "DIRECCION", length = 255)
    private String direccion;

    @Column(name = "FECHA_REGISTRO")
    private LocalDateTime fechaRegistro;

    // Relación con Rol para obtener nombre
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ID_ROL", insertable = false, updatable = false)
    private RolEntity rol;
}
