package com.plantopolis.backend.infrastructure.adapter.in.web;

import com.plantopolis.backend.domain.port.in.GestionarUsuariosAdminUseCase;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.ResetPasswordResponse;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.UsuarioAdminResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/usuarios")
@RequiredArgsConstructor
@Tag(
    name = "Admin — Usuarios",
    description = "Gestión de cuentas de usuario: listar, activar/desactivar y resetear " +
                  "contraseñas. 🔒 Requiere token JWT + rol ADMINISTRADOR."
)
@SecurityRequirement(name = "Bearer Authentication")
public class AdminUsuarioController {

    private final GestionarUsuariosAdminUseCase usuariosUseCase;

    @Operation(
        summary = "Listar usuarios",
        description = "Devuelve una página de usuarios con filtros opcionales por " +
                      "nombre/correo, rol (1=ADMINISTRADOR, 2=TRABAJADOR, 3=CLIENTE) y estado."
    )
    @ApiResponse(responseCode = "200", description = "Lista de usuarios paginada")
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Page<UsuarioAdminResponse>> listar(
            @Parameter(description = "Filtro por nombre o correo (parcial)")
            @RequestParam(required = false) String nombre,
            @Parameter(description = "Filtro por ID de rol")
            @RequestParam(required = false) Long idRol,
            @Parameter(description = "Filtro por estado de la cuenta")
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by("nombreCompleto").ascending());
        var resultado = usuariosUseCase
                .listarUsuarios(nombre, idRol, activo, pageable)
                .map(UsuarioAdminResponse::from);
        return ResponseEntity.ok(resultado);
    }

    @Operation(
        summary = "Activar cuenta de usuario",
        description = "Restaura el acceso de un usuario previamente desactivado."
    )
    @ApiResponse(responseCode = "200", description = "Usuario activado")
    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UsuarioAdminResponse> activar(@PathVariable Long id) {
        return ResponseEntity.ok(UsuarioAdminResponse.from(usuariosUseCase.activar(id)));
    }

    @Operation(
        summary = "Desactivar cuenta de usuario",
        description = "Revoca el acceso del usuario (no podrá volver a iniciar sesión hasta " +
                      "que se reactive). No borra su historial de pedidos ni tareas."
    )
    @ApiResponse(responseCode = "200", description = "Usuario desactivado")
    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UsuarioAdminResponse> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(UsuarioAdminResponse.from(usuariosUseCase.desactivar(id)));
    }

    @Operation(
        summary = "Resetear contraseña",
        description = "Genera una contraseña temporal aleatoria y la asigna al usuario. " +
                      "Se devuelve una única vez en la respuesta — el admin debe comunicarla " +
                      "de forma segura, ya que no vuelve a mostrarse."
    )
    @ApiResponse(responseCode = "200", description = "Contraseña reseteada")
    @PostMapping("/{id}/resetear-password")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ResetPasswordResponse> resetearPassword(@PathVariable Long id) {
        String nueva = usuariosUseCase.resetearPassword(id);
        return ResponseEntity.ok(new ResetPasswordResponse(
                "Contraseña reseteada correctamente. Compártela de forma segura con el usuario.",
                nueva
        ));
    }
}