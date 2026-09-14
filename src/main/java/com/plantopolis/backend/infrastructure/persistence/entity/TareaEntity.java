package com.plantopolis.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "TAREA")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TareaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_TAREA")
    private Long idTarea;

    @Column(name = "ID_TAREA_RECURRENTE")
    private Long idTareaRecurrente;

    @Column(name = "TIPO_TAREA", nullable = false, length = 50)
    private String tipoTarea;

    @Column(name = "PRIORIDAD", nullable = false, length = 20)
    private String prioridad;

    @Column(name = "ESTADO_TAREA", nullable = false, length = 40)
    private String estadoTarea;

    @Column(name = "ID_ZONA")
    private Long idZona;

    @Column(name = "ID_LOTE")
    private Long idLote;

    @Column(name = "ID_TRABAJADOR_ASIGNADO")
    private Long idTrabajadorAsignado;

    @Column(name = "FECHA_LIMITE", nullable = false)
    private LocalDate fechaLimite;

    @Column(name = "ALERTA_VENCIDA_ENVIADA", nullable = false)
    private Boolean alertaVencidaEnviada;

    @Column(name = "FECHA_CREACION", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "FECHA_COMPLETADA")
    private LocalDateTime fechaCompletada;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ID_ZONA", insertable = false, updatable = false)
    private ZonaEntity zona;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ID_LOTE", insertable = false, updatable = false)
    private LoteProduccionEntity lote;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ID_TRABAJADOR_ASIGNADO", insertable = false, updatable = false)
    private UsuarioEntity trabajador;
}
