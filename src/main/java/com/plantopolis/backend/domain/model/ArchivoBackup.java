package com.plantopolis.backend.domain.model;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
/** Representa el archivo SQL de backup generado. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ArchivoBackup {
    private String nombreArchivo;
    private byte[] contenido;
    private LocalDateTime fechaGeneracion;
    private List<TablaRespaldo> tablas;
    private long totalFilas;
}
