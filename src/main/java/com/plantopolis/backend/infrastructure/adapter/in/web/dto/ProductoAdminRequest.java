package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * DTO de entrada para crear o actualizar un producto desde el panel admin.
 *
 * Se usa en:
 *   POST /api/admin/productos       → crear producto
 *   PUT  /api/admin/productos/{id}  → actualizar producto
 *
 * Para actualización (PUT), solo los campos enviados se actualizan.
 * Los campos null son ignorados por AdminProductoService.actualizar().
 *
 */
@Schema(description = "Datos para crear o actualizar un producto (solo admin)")
public record ProductoAdminRequest(

        @Schema(description = "Nombre del producto",
                example = "Pothos Dorado Premium")
        @NotBlank(message = "El nombre del producto es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String nombreProducto,

        @Schema(description = "Descripción detallada del producto",
                example = "Planta tropical de fácil cuidado, ideal para interiores")
        String descripcion,

        @Schema(description = "Precio de venta en pesos colombianos",
                example = "25000.00")
        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
        BigDecimal precio,

        @Schema(description = "Unidades disponibles en inventario",
                example = "50")
        @NotNull(message = "El stock es obligatorio")
        @Min(value = 0, message = "El stock no puede ser negativo")
        Integer stock,

        @Schema(description = "URL de la imagen principal del producto",
                example = "https://example.com/img/pothos.jpg")
        String imagenUrl,

        @Schema(description = "ID de la categoría del producto",
                example = "1")
        @NotNull(message = "La categoría es obligatoria")
        Long idCategoria,

        @Schema(description = "ID del tipo de producto",
                example = "1")
        @NotNull(message = "El tipo es obligatorio")
        Long idTipo,

        @Schema(description = "Instrucciones de cuidado de la planta",
                example = "Regar dos veces por semana, evitar luz directa")
        String cuidados,

        @Schema(description = "Requisito de luz",
                example = "Luz indirecta brillante")
        String luz,

        @Schema(description = "Frecuencia de riego",
                example = "Cada 3-4 días")
        String riego,

        @Schema(description = "Tamaño aproximado de la planta",
                example = "30-50 cm")
        String tamanioEstimado
) {}