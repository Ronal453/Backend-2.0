package com.plantopolis.backend.infrastructure.adapter.in.web;

import com.plantopolis.backend.domain.port.in.LoginUseCase;
import com.plantopolis.backend.domain.port.in.RegistrarUsuarioUseCase;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.AuthResponse;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.LoginRequest;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.RegistroRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(
    name = "Autenticación",
    description = "Registro e inicio de sesión de usuarios. " +
                  "El token JWT obtenido debe usarse en el header: Authorization: Bearer {token}"
)
public class AuthController {

    private final RegistrarUsuarioUseCase registrarUseCase;
    private final LoginUseCase            loginUseCase;
    private final PasswordEncoder         passwordEncoder;

    // ── REGISTRO ─────────────────────────────────────────────
    @Operation(
        summary = "Registrar nuevo usuario",
        description = "Crea una cuenta nueva con rol CLIENTE. " +
                      "El email debe ser único en el sistema."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Usuario registrado correctamente",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "El email ya está registrado",
            content = @Content(examples = @ExampleObject(
                value = """
                    {
                      "status": 409,
                      "mensaje": "El email ya está registrado: ejemplo@email.com"
                    }
                    """
            ))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos inválidos (campos vacíos, email mal formado, contraseña corta)"
        )
    })
    @PostMapping("/registro")
    public ResponseEntity<AuthResponse> registro(
            @Valid @RequestBody RegistroRequest request) {
        var usuario = registrarUseCase.registrar(
                request.nombre(), request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(
                        null,
                        usuario.getCorreo(),
                        "CLIENTE",
                        "Usuario registrado exitosamente"));
    }

    // ── LOGIN ────────────────────────────────────────────────
    @Operation(
        summary = "Iniciar sesión",
        description = "Autentica un usuario y devuelve un token JWT válido por 24 horas. " +
                      "Usa el token en el botón **Authorize** de esta página."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Login exitoso — copia el token JWT",
            content = @Content(
                schema = @Schema(implementation = AuthResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                          "email": "usuario@email.com",
                          "rol": "CLIENTE",
                          "mensaje": "Login exitoso"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Credenciales incorrectas"
        )
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        String token = loginUseCase.login(request.email(), request.password());
        return ResponseEntity.ok(new AuthResponse(
                token, request.email(), null, "Login exitoso"));
    }
}