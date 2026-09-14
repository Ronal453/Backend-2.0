package com.plantopolis.backend.infrastructure.adapter.in.web;

import com.plantopolis.backend.domain.port.in.GestionarMermasTrabajadorUseCase;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.CausaMermaResponse;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.MermaResponse;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.RegistrarMermaRequest;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mermas")
@RequiredArgsConstructor
@Tag(
        name = "Trabajador — Mermas y Pérdidas",
        description = "Registro y consulta de mermas/pérdidas de producción con descuento automático de lote. " +
                      "🔒 Requiere token JWT con rol TRABAJADOR o ADMINISTRADOR."
)
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasAnyRole('TRABAJADOR', 'ADMINISTRADOR')")
public class TrabajadorMermaController {

    private final GestionarMermasTrabajadorUseCase mermasUseCase;

    @Operation(summary = "Listar causas de merma oficiales")
    @GetMapping("/causas")
    public ResponseEntity<List<CausaMermaResponse>> listarCausas() {
        var causas = mermasUseCase.listarCausasMerma().stream()
                .map(CausaMermaResponse::from)
                .toList();
        return ResponseEntity.ok(causas);
    }

    @Operation(
            summary = "Registrar merma en un lote",
            description = "Descuenta la cantidad perdida del total del lote y almacena el registro de la pérdida."
    )
    @ApiResponse(responseCode = "201", description = "Merma registrada correctamente")
    @PostMapping
    public ResponseEntity<MermaResponse> registrarMerma(
            @Valid @RequestBody RegistrarMermaRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        var merma = mermasUseCase.registrarMerma(
                request.idLote(),
                request.idCausa(),
                request.cantidadPerdida(),
                request.fechaMerma(),
                request.observaciones(),
                user.getUsername()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(MermaResponse.from(merma));
    }

    @Operation(summary = "Listar mermas recientes")
    @GetMapping
    public ResponseEntity<List<MermaResponse>> listarRecientes(
            @RequestParam(defaultValue = "20") int limite
    ) {
        var lista = mermasUseCase.listarMermasRecientes(limite).stream()
                .map(MermaResponse::from)
                .toList();
        return ResponseEntity.ok(lista);
    }
}
