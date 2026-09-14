package com.plantopolis.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "LOTEPRODUCCION")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoteProduccionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_LOTE")
    private Long idLote;

    @Column(name = "CODIGO_LOTE", nullable = false, unique = true, length = 50)
    private String codigoLote;

    @Column(name = "ESPECIE", nullable = false, length = 100)
    private String especie;

    @Column(name = "CANTIDAD_INICIAL", nullable = false)
    private Integer cantidadInicial;

    @Column(name = "CANTIDAD_ACTUAL", nullable = false)
    private Integer cantidadActual;

    @Column(name = "FECHA_SIEMBRA", nullable = false)
    private LocalDate fechaSiembra;

    @Column(name = "ESTADO_LOTE", nullable = false, length = 40)
    private String estadoLote;

    @Column(name = "ID_ZONA", nullable = false)
    private Long idZona;

    @Column(name = "ID_PROVEEDOR")
    private Long idProveedor;

    @Column(name = "ID_PRODUCTO")
    private Long idProducto;

    @Column(name = "ES_VINCULADO", nullable = false)
    private Boolean esVinculado;

    @Column(name = "FECHA_VINCULACION")
    private LocalDateTime fechaVinculacion;

    @Column(name = "FECHA_CREACION", nullable = false)
    private LocalDateTime fechaCreacion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ID_ZONA", insertable = false, updatable = false)
    private ZonaEntity zona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_PRODUCTO", insertable = false, updatable = false)
    private ProductoEntity producto;
}
