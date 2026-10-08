package com.plantopolis.backend.domain.model;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
/** Modelo que representa una tabla y la cantidad de filas en un backup. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TablaRespaldo {
    private String nombreTabla;
    private long filas;
}
