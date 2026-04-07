package com.plantopolis.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "PRODUCTO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_PRODUCTO")
    private Long idProducto;

    @Column(name = "ID_CATEGORIA", nullable = false)
    private Long idCategoria;

    @Column(name = "ID_TIPO", nullable = false)
    private Long idTipo;

    @Column(name = "NOMBRE_PRODUCTO", nullable = false, length = 150)
    private String nombreProducto;

    @Lob                          // ← CLOB en Oracle
    @Column(name = "DESCRIPCION")
    private String descripcion;

    @Column(name = "PRECIO", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "STOCK", nullable = false)
    private Integer stock;

    @Column(name = "IMAGEN_URL", length = 300)
    private String imagenUrl;

    @Lob                          // ← CLOB en Oracle
    @Column(name = "CUIDADOS")
    private String cuidados;

    @Column(name = "LUZ", length = 100)
    private String luz;

    @Column(name = "RIEGO", length = 100)
    private String riego;

    @Column(name = "TAMANIO_ESTIMADO", length = 100)
    private String tamanioEstimado;

    @Column(name = "ACTIVO", nullable = false)
    private Boolean activo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ID_CATEGORIA", insertable = false, updatable = false)
    private CategoriaEntity categoria;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ID_TIPO", insertable = false, updatable = false)
    private TipoProductoEntity tipo;
}