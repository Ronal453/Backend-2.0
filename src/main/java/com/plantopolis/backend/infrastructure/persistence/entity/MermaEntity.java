package com.plantopolis.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "MERMA")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MermaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_MERMA")
    private Long idMerma;

    @Column(name = "ID_LOTE", nullable = false)
    private Long idLote;

    @Column(name = "ID_CAUSA", nullable = false)
    private Long idCausa;

    @Column(name = "ID_USUARIO", nullable = false)
    private Long idUsuario;

    @Column(name = "CANTIDAD_PERDIDA", nullable = false)
    private Integer cantidadPerdida;

    @Column(name = "FECHA_MERMA", nullable = false)
    private LocalDate fechaMerma;

    /**
     * Detalle u observaciones sobre la pérdida sufrida en el lote.
     * Mapeado como TEXT para compatibilidad directa con PostgreSQL.
     */
    @Column(name = "OBSERVACIONES", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "FECHA_REGISTRO", nullable = false)
    private LocalDateTime fechaRegistro;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ID_LOTE", insertable = false, updatable = false)
    private LoteProduccionEntity lote;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ID_CAUSA", insertable = false, updatable = false)
    private CausaMermaEntity causa;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ID_USUARIO", insertable = false, updatable = false)
    private UsuarioEntity usuario;
}
