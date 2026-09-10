package com.plantopolis.backend.infrastructure.persistence.mapper;

import com.plantopolis.backend.domain.model.Usuario;
import com.plantopolis.backend.infrastructure.persistence.entity.UsuarioEntity;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    public Usuario toDomain(UsuarioEntity entity) {
        if (entity == null) return null;
        return Usuario.builder()
                .idUsuario(entity.getIdUsuario())
                .idRol(entity.getIdRol())
                .nombreCompleto(entity.getNombreCompleto())
                .correo(entity.getCorreo())
                .contrasenaHash(entity.getContrasenaHash())
                .telefono(entity.getTelefono())
                .direccion(entity.getDireccion())
                .fechaRegistro(entity.getFechaRegistro())
                .rolNombre(entity.getRol() != null ? entity.getRol().getNombre() : null)
                .activo(entity.getActivo())
                .fechaActualizacion(entity.getFechaActualizacion())
                .build();
    }

    public UsuarioEntity toEntity(Usuario domain) {
        if (domain == null) return null;
        return UsuarioEntity.builder()
                .idUsuario(domain.getIdUsuario())
                .idRol(domain.getIdRol())
                .nombreCompleto(domain.getNombreCompleto())
                .correo(domain.getCorreo())
                .contrasenaHash(domain.getContrasenaHash())
                .telefono(domain.getTelefono())
                .direccion(domain.getDireccion())
                .fechaRegistro(domain.getFechaRegistro())
                .activo(domain.getActivo())
                .fechaActualizacion(domain.getFechaActualizacion())
                .build();
    }
}