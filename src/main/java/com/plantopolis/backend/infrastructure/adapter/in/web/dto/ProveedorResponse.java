package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import com.plantopolis.backend.domain.model.Proveedor;

import java.time.LocalDateTime;

/**
 * Respuesta REST con los datos de un proveedor.
 */
public record ProveedorResponse(
        Long idProveedor,
        String nombre,
        String contacto,
        String telefono,
        String correo,
        String tipoInsumo,
        Boolean activo,
        LocalDateTime fechaCreacion
) {
    /**
     * @param p modelo de dominio
     * @return DTO equivalente o null
     */
    public static ProveedorResponse from(Proveedor p) {
        if (p == null) return null;
        return new ProveedorResponse(
                p.getIdProveedor(),
                p.getNombre(),
                p.getContacto(),
                p.getTelefono(),
                p.getCorreo(),
                p.getTipoInsumo(),
                p.getActivo(),
                p.getFechaCreacion()
        );
    }
}
