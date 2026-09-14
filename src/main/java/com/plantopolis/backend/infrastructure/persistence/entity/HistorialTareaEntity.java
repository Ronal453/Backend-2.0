package com.plantopolis.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "HISTORIALTAREA")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialTareaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_HISTORIAL_TAREA")
    private Long idHistorialTarea;

    @Column(name = "ID_TAREA", nullable = false)
    private Long idTarea;

    @Column(name = "ID_USUARIO", nullable = false)
    private Long idUsuario;

    @Column(name = "TIPO_CAMBIO", nullable = false, length = 30)
    private String tipoCambio;

    @Column(name = "ESTADO_ANTERIOR", length = 40)
    private String estadoAnterior;

    @Column(name = "ESTADO_NUEVO", length = 40)
    private String estadoNuevo;

    @Column(name = "ID_TRABAJADOR_ANTERIOR")
    private Long idTrabajadorAnterior;

    @Column(name = "ID_TRABAJADOR_NUEVO")
    private Long idTrabajadorNuevo;

    @Column(name = "FECHA_CAMBIO", nullable = false)
    private LocalDateTime fechaCambio;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ID_USUARIO", insertable = false, updatable = false)
    private UsuarioEntity usuario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ID_TRABAJADOR_ANTERIOR", insertable = false, updatable = false)
    private UsuarioEntity trabajadorAnterior;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ID_TRABAJADOR_NUEVO", insertable = false, updatable = false)
    private UsuarioEntity trabajadorNuevo;
}
