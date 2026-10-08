package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import com.plantopolis.backend.domain.model.Proveedor;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload para crear o actualizar un proveedor (HU34).
 * Los límites de longitud reflejan las columnas VARCHAR2 de la tabla PROVEEDOR.
 *
 * @param nombre     razón social o nombre comercial (obligatorio)
 * @param contacto   persona de contacto (opcional)
 * @param telefono   teléfono con dígitos, espacios, guiones, paréntesis y '+' (opcional)
 * @param correo     correo electrónico válido (opcional)
 * @param tipoInsumo SEMILLAS, SUSTRATOS, MACETAS u OTROS (obligatorio)
 */
public record ProveedorRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String nombre,

        @Size(max = 100, message = "El contacto no puede superar 100 caracteres")
        String contacto,

        @Size(max = 20, message = "El teléfono no puede superar 20 caracteres")
        @Pattern(regexp = "^$|^[+0-9()\\-\\s]{7,20}$", message = "El teléfono solo admite dígitos, espacios, guiones, paréntesis y '+'")
        String telefono,

        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 150, message = "El correo no puede superar 150 caracteres")
        String correo,

        @NotBlank(message = "El tipo de insumo es obligatorio")
        String tipoInsumo
) {
    /** Convierte el payload al modelo de dominio (sin id, estado ni fecha: los define el servicio). */
    public Proveedor toDomain() {
        return Proveedor.builder()
                .nombre(nombre)
                .contacto(contacto)
                .telefono(telefono)
                .correo(correo)
                .tipoInsumo(tipoInsumo)
                .build();
    }
}
