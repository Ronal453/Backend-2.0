package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Schema(description = "Datos para crear o actualizar un producto (solo admin)")
public record ProductoAdminRequest(

        @NotBlank(message = "El nombre del producto es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String nombreProducto,

        String descripcion,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
        BigDecimal precio,

        @NotNull(message = "El stock es obligatorio")
        @Min(value = 0, message = "El stock no puede ser negativo")
        Integer stock,

        String imagenUrl,

        @NotNull(message = "La categoría es obligatoria")
        Long idCategoria,

        @NotNull(message = "El tipo es obligatorio")
        Long idTipo,

        String cuidados,
        String luz,
        String riego,
        String tamanioEstimado,

        @Schema(description = "Umbral de stock crítico. Opcional — si no se envía, se " +
                              "mantiene el valor actual (por defecto 5 en BD).", example = "5")
        @Min(value = 0, message = "El umbral de alerta no puede ser negativo")
        Integer stockMinimoAlerta
) {}