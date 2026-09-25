package com.plantopolis.backend.infrastructure.adapter.in.web;

import com.plantopolis.backend.domain.model.LoteProduccion;
import com.plantopolis.backend.domain.port.in.GestionarLotesAdminUseCase;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.CrearLoteRequest;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.LoteResponse;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.VincularLoteRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/lotes")
@RequiredArgsConstructor
@Tag(
        name = "Admin — Lotes de Producción",
        description = "Gestión de lotes de cultivo y vinculación de stock al catálogo. 🔒 Requiere token JWT con rol ADMINISTRADOR."
)
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminLoteController {

    private final GestionarLotesAdminUseCase gestionarLotesAdminUseCase;

    @Operation(
            summary = "Registrar lote de producción",
            description = "Crea un nuevo lote en estado GERMINANDO, validando capacidad de la zona."
    )
    @ApiResponse(responseCode = "201", description = "Lote creado exitosamente")
    @PostMapping
    public ResponseEntity<LoteResponse> registrarLote(
            @Valid @RequestBody CrearLoteRequest request,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails user
    ) {
        try {
            LoteProduccion loteNuevo = LoteProduccion.builder()
                    .especie(request.especie())
                    .cantidadInicial(request.cantidadInicial())
                    .fechaSiembra(request.fechaSiembra())
                    .idZona(request.idZona())
                    .codigoLote(request.codigoLote())
                    .idProveedor(request.idProveedor())
                    .estadoLote(request.estadoLote())
                    .build();

            LoteProduccion loteGuardado = gestionarLotesAdminUseCase.registrarLote(loteNuevo, user.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(LoteResponse.from(loteGuardado));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        }
    }

    @Operation(
            summary = "Vincular lote con producto del catálogo",
            description = "Suma el inventario de un lote LISTO_PARA_VENTA al stock de un producto."
    )
    @ApiResponse(responseCode = "200", description = "Lote vinculado exitosamente")
    @ApiResponse(responseCode = "409", description = "El lote está DESCARTADO o ya fue vinculado (Conflicto)")
    @PostMapping("/{id}/vincular")
    public ResponseEntity<LoteResponse> vincularLote(
            @PathVariable Long id,
            @Valid @RequestBody VincularLoteRequest request
    ) {
        try {
            LoteProduccion loteVinculado = gestionarLotesAdminUseCase.vincularLoteConProducto(id, request.idProducto());
            return ResponseEntity.ok(LoteResponse.from(loteVinculado));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e); // 409 para HU16b
        }
    }
}
