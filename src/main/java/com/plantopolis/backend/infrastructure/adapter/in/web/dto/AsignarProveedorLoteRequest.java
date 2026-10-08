package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

/**
 * Payload para asociar un lote existente con su proveedor (HU35).
 *
 * @param idProveedor proveedor activo a asignar; {@code null} quita la asociación (campo opcional)
 */
public record AsignarProveedorLoteRequest(
        Long idProveedor
) {}
