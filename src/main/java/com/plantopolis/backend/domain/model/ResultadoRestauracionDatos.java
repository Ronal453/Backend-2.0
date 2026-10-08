package com.plantopolis.backend.domain.model;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
/** DTO interno para retornar desde el puerto de repositorio. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ResultadoRestauracionDatos {
    private List<TablaRespaldo> tablas;
    private long totalFilas;
    private List<String> advertencias;
}
