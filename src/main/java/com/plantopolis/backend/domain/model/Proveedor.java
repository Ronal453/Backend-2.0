package com.plantopolis.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Modelo de dominio puro de un proveedor de insumos agrícolas (HU34 / RF33).
 * <p>
 * Representa una fuente de abastecimiento del vivero (semillas, sustratos, macetas, etc.).
 * No depende de JPA: la persistencia se resuelve en {@code ProveedorEntity} vía mapper.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Proveedor {

    /** Catálogo cerrado de tipos de insumo aceptados (coincide con el comentario de la columna TIPO_INSUMO). */
    public static final Set<String> TIPOS_INSUMO_VALIDOS = Set.of("SEMILLAS", "SUSTRATOS", "MACETAS", "PLANTAS", "OTROS");

    private Long idProveedor;
    private String nombre;
    private String contacto;
    private String telefono;
    private String correo;
    private String tipoInsumo;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
}
