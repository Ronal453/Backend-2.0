package com.plantopolis.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "HISTORIALESTADOLOTE")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialEstadoLoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_HISTORIAL_LOTE")
    private Long idHistorialLote;

    @Column(name = "ID_LOTE", nullable = false)
    private Long idLote;

    @Column(name = "ID_USUARIO", nullable = false)
    private Long idUsuario;

    @Column(name = "ESTADO_ANTERIOR", nullable = false, length = 40)
    private String estadoAnterior;

    @Column(name = "ESTADO_NUEVO", nullable = false, length = 40)
    private String estadoNuevo;

    @Lob
    @Column(name = "OBSERVACIONES")
    private String observaciones;

    @Column(name = "FECHA_CAMBIO", nullable = false)
    private LocalDateTime fechaCambio;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ID_USUARIO", insertable = false, updatable = false)
    private UsuarioEntity usuario;
}
