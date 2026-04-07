package com.plantopolis.backend.domain.model;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipoProducto {
    private Long idTipo;
    private String nombreTipo;
}
