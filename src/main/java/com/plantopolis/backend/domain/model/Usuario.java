package com.plantopolis.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {
    private Long idUsuario;
    private Long idRol;
    private String nombreCompleto;
    private String correo;
    private String contrasenaHash;
    private String telefono;
    private String direccion;
    private String rolNombre;
}