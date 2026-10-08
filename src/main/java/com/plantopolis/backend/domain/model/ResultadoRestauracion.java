package com.plantopolis.backend.domain.model;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
/** Representa el resultado de la restauracion para mostrar al usuario. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ResultadoRestauracion {
    private String nombreArchivo;
    private LocalDateTime fechaRestauracion;
    private List<TablaRespaldo> tablas;
    private long totalFilas;
    private long duracionMs;
    private List<String> advertencias;
}
