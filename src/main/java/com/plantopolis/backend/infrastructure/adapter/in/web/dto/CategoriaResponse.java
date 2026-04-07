package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import com.plantopolis.backend.domain.model.Categoria;

public record CategoriaResponse(
        Long idCategoria,
        String nombreCategoria,
        String descripcion
) {
    public static CategoriaResponse from(Categoria c) {
        return new CategoriaResponse(
                c.getIdCategoria(),
                c.getNombreCategoria(),
                c.getDescripcion()
        );
    }
}
