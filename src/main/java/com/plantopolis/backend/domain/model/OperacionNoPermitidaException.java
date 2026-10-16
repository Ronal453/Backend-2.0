package com.plantopolis.backend.domain.model;

/**
 * Excepción de dominio lanzada cuando una operación viola una regla de negocio o
 * restricción de propiedad (ej. un trabajador intentando mover una tarea ajena).
 * Mapea a código HTTP 403 Forbidden.
 */
public class OperacionNoPermitidaException extends RuntimeException {
    public OperacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
