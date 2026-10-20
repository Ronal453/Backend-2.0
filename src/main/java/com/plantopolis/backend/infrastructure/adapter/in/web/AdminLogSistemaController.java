package com.plantopolis.backend.infrastructure.adapter.in.web;

import com.plantopolis.backend.domain.model.ResumenLogsSistema;
import com.plantopolis.backend.domain.port.in.ConsultarLogsSistemaUseCase;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.ResumenLogsSistemaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

/**
 * Controlador REST exclusivo para el rol ADMINISTRADOR que expone los registros de actividad,
 * bitácora de auditoría y eventos internos del sistema Plantopolis.
 */
@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
@Tag(name = "Admin — Logs del Sistema", description = "Supervisión y auditoría de las acciones ejecutadas por el sistema. Requiere rol ADMINISTRADOR.")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminLogSistemaController {

    private final ConsultarLogsSistemaUseCase logsSistemaUseCase;

    /**
     * Obtiene el listado consolidado de logs del sistema junto con las métricas de resumen,
     * permitiendo filtrar por texto libre, módulo, nivel de severidad y origen.
     *
     * @param busqueda Texto opcional para filtrar por acción, usuario, descripción o endpoint.
     * @param modulo   Módulo funcional opcional (AUTH, USUARIOS, PRODUCTOS, PEDIDOS, TAREAS, LOTES, MERMAS, ZONAS, PROVEEDORES, BACKUP, SISTEMA).
     * @param nivel    Nivel de severidad opcional (INFO, SUCCESS, WARN, ERROR).
     * @param origen   Fuente del registro opcional (API_HTTP, AUDITORIA_BD, MOTOR_SISTEMA).
     * @param limite   Cantidad máxima de registros a devolver (por defecto 300).
     * @return {@link ResumenLogsSistemaResponse} con métricas y listado de eventos.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Consulta los logs y acciones del sistema con filtros opcionales")
    public ResponseEntity<ResumenLogsSistemaResponse> consultarLogs(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String modulo,
            @RequestParam(required = false) String nivel,
            @RequestParam(required = false) String origen,
            @RequestParam(defaultValue = "300") int limite
    ) {
        ResumenLogsSistema resumen = logsSistemaUseCase.consultarLogs(busqueda, modulo, nivel, origen, limite);
        return ResponseEntity.ok(ResumenLogsSistemaResponse.from(resumen));
    }

    /**
     * Limpia el buffer de eventos transitorios en memoria (conservando intacta la auditoría
     * persistida en base de datos).
     *
     * @param principal Identidad del administrador autenticado.
     * @return Confirmación de la limpieza realizada.
     */
    @DeleteMapping("/memoria")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Limpia los logs transitorios en memoria del servidor")
    public ResponseEntity<Map<String, String>> limpiarLogsEnMemoria(Principal principal) {
        String admin = (principal != null && principal.getName() != null)
                ? principal.getName() : "ADMINISTRADOR";
        logsSistemaUseCase.limpiarLogsEnMemoria(admin);
        return ResponseEntity.ok(Map.of("mensaje", "Buffer de logs en memoria reiniciado correctamente."));
    }
}
