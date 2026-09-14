package com.plantopolis.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CausaMerma {
    private Long idCausa;
    private String nombreCausa;
    private Boolean activo;
}
