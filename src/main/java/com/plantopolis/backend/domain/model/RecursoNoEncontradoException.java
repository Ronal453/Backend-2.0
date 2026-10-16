package com.plantopolis.backend.domain.model;

/**
 * Excepción de dominio lanzada cuando una entidad o recurso solicitado no existe
 * (ej. tarea no encontrada, usuario inexistente). Mapea a código HTTP 404.
 */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
