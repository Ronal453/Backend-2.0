package com.plantopolis.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "COMENTARIOTAREA")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComentarioTareaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_COMENTARIO")
    private Long idComentario;

    @Column(name = "ID_TAREA", nullable = false)
    private Long idTarea;

    @Column(name = "ID_USUARIO", nullable = false)
    private Long idUsuario;

    @Lob
    @Column(name = "COMENTARIO", nullable = false)
    private String comentario;

    @Column(name = "FECHA_CREACION", nullable = false)
    private LocalDateTime fechaCreacion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ID_USUARIO", insertable = false, updatable = false)
    private UsuarioEntity usuario;
}
