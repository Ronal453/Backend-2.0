package com.plantopolis.backend.infrastructure.adapter.in.web;

import com.plantopolis.backend.domain.model.ArchivoBackup;
import com.plantopolis.backend.domain.model.ResultadoRestauracion;
import com.plantopolis.backend.domain.port.in.GestionarBackupAdminUseCase;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.ResultadoRestauracionResponse;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.TablaRespaldoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/admin/backup")
@RequiredArgsConstructor
@Tag(name = "Admin — Backup", description = "Gestion de copias de seguridad. Requiere rol ADMINISTRADOR.")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminBackupController {

    private final GestionarBackupAdminUseCase backupUseCase;

    @GetMapping("/generar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Genera copia de seguridad SQL")
    public ResponseEntity<byte[]> generarBackup(@AuthenticationPrincipal UserDetails userDetails) {
        ArchivoBackup archivo = backupUseCase.generarBackup(userDetails.getUsername());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archivo.getNombreArchivo() + "\"")
                .contentType(MediaType.parseMediaType("application/sql; charset=UTF-8"))
                .body(archivo.getContenido());
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Obtiene resumen del estado actual de la BD")
    public ResponseEntity<List<TablaRespaldoResponse>> obtenerResumen() {
        return ResponseEntity.ok(TablaRespaldoResponse.from(backupUseCase.obtenerResumenActual()));
    }

    @PostMapping(value = "/restaurar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Restaura la BD desde un archivo .sql")
    public ResponseEntity<ResultadoRestauracionResponse> restaurarBackup(
            @RequestParam("archivo") MultipartFile archivo,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        ResultadoRestauracion res = backupUseCase.restaurarBackup(archivo.getOriginalFilename(), archivo.getBytes(), userDetails.getUsername());
        return ResponseEntity.ok(ResultadoRestauracionResponse.from(res));
    }
}
