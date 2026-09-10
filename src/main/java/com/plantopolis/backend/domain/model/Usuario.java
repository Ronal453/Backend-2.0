package com.plantopolis.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {
    private Long          idUsuario;
    private Long          idRol;
    private String        nombreCompleto;
    private String        correo;
    private String        contrasenaHash;
    private String        telefono;
    private String        direccion;
    private LocalDateTime fechaRegistro;
    private String        rolNombre;

    // reflejan ACTIVO y FECHA_ACTUALIZACION de USUARIO.
    private Boolean       activo;
    private LocalDateTime fechaActualizacion;
}