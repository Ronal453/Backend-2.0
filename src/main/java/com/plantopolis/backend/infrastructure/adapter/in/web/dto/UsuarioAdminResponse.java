package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import com.plantopolis.backend.domain.model.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Datos de un usuario para el panel de administración")
public record UsuarioAdminResponse(
        Long idUsuario,
        String nombreCompleto,
        String correo,
        String telefono,
        String direccion,
        String rol,
        Boolean activo,
        LocalDateTime fechaRegistro
) {
    public static UsuarioAdminResponse from(Usuario u) {
        return new UsuarioAdminResponse(
                u.getIdUsuario(),
                u.getNombreCompleto(),
                u.getCorreo(),
                u.getTelefono(),
                u.getDireccion(),
                u.getRolNombre(),
                u.getActivo(),
                u.getFechaRegistro()
        );
    }
}