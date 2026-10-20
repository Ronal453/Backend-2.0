package com.plantopolis.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * Entidad JPA para la tabla PRODUCTO.
 */
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

    /**
     * Descripción comercial y botánica del producto.
     * Se usa columnDefinition = "TEXT" en lugar de @Lob para evitar errores de OID en PostgreSQL.
     */
    @Column(name = "DESCRIPCION", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "PRECIO", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    // porcentaje de IVA aplicable, default 19.00 en BD.
    @Column(name = "PORCENTAJE_IVA", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeIva;

    @Column(name = "STOCK", nullable = false)
    private Integer stock;

    // umbral de alerta de stock bajo.
    @Column(name = "STOCK_MINIMO_ALERTA", nullable = false)
    private Integer stockMinimoAlerta;

    @Column(name = "IMAGEN_URL", length = 300)
    private String imagenUrl;

    /**
     * Recomendaciones detalladas de cuidado para la ficha técnica.
     * Se usa columnDefinition = "TEXT" en lugar de @Lob para compatibilidad con PostgreSQL.
     */
    @Column(name = "CUIDADOS", columnDefinition = "TEXT")
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
