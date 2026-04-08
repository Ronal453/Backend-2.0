package com.plantopolis.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "CARRITOITEM")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarritoItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_ITEM")
    private Long idItem;

    @Column(name = "ID_CARRITO", nullable = false)
    private Long idCarrito;

    @Column(name = "ID_PRODUCTO", nullable = false)
    private Long idProducto;

    @Column(name = "CANTIDAD", nullable = false)
    private Integer cantidad;

    // Relación con carrito
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_CARRITO",
                insertable = false, updatable = false)
    private CarritoEntity carrito;

    // Relación con producto para obtener datos
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ID_PRODUCTO",
                insertable = false, updatable = false)
    private ProductoEntity producto;
}