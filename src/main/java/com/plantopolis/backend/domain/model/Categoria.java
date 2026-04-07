package com.plantopolis.backend.domain.model;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Categoria {
    private Long idCategoria;
    private String nombreCategoria;
    private String descripcion;
}
