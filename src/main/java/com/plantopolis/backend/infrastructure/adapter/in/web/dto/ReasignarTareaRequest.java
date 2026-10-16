package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

/**
 * Petición para reasignar una tarea operativa a otro trabajador o enviarla al backlog (HU30).
 * Soporta tanto {@code idTrabajador} como {@code nuevoIdAsignadoA} para interoperabilidad con el cliente web.
 */
public record ReasignarTareaRequest(
        Long idTrabajador,
        Long nuevoIdAsignadoA,
        String motivo
) {
    public ReasignarTareaRequest(Long idTrabajador) {
        this(idTrabajador, idTrabajador, null);
    }

    public Long resolverIdTrabajador() {
        return idTrabajador != null ? idTrabajador : nuevoIdAsignadoA;
    }
}
