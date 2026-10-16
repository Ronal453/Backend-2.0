package com.plantopolis.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad JPA para la tabla TAREARECURRENTE (HU23).
 */
@Entity
@Table(name = "TAREARECURRENTE")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TareaRecurrenteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_TAREA_RECURRENTE")
    private Long idTareaRecurrente;

    @Column(name = "TIPO_TAREA", nullable = false, length = 50)
    private String tipoTarea;

    @Column(name = "PRIORIDAD", nullable = false, length = 20)
    private String prioridad;

    @Column(name = "DESCRIPCION", length = 500)
    private String descripcion;

    @Column(name = "FRECUENCIA_DIAS", nullable = false)
    private Integer frecuenciaDias;

    @Column(name = "ID_ZONA")
    private Long idZona;

    @Column(name = "ID_LOTE")
    private Long idLote;

    @Column(name = "ID_TRABAJADOR_ASIGNADO")
    private Long idTrabajadorAsignado;

    @Column(name = "ACTIVO", nullable = false)
    private Boolean activo;

    @Column(name = "FECHA_PROXIMA_EJECUCION", nullable = false)
    private LocalDate fechaProximaEjecucion;

    @Column(name = "FECHA_CREACION", nullable = false)
    private LocalDateTime fechaCreacion;

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
