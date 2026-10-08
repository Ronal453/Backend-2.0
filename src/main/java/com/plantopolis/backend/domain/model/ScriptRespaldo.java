package com.plantopolis.backend.domain.model;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
/** Modelo que representa el resultado de la exportacion SQL desde la BD. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ScriptRespaldo {
    private String sql;
    private List<TablaRespaldo> tablas;
    private long totalFilas;
}
