package com.plantopolis.backend.domain.model;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
/** Representa una sentencia de restauracion extraida del archivo SQL. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SentenciaRestauracion {
    private String tabla;
    private String sql;
    private int numeroLinea;
}
