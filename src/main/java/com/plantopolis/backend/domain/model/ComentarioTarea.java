package com.plantopolis.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComentarioTarea {
    private Long idComentario;
    private Long idTarea;
    private Long idUsuario;
    private String nombreUsuario;
    private String comentario;
    private LocalDateTime fechaCreacion;
}
