package com.plantopolis.backend.infrastructure.adapter.in.web;

import com.plantopolis.backend.domain.port.in.LoginGoogleUseCase;
import com.plantopolis.backend.domain.port.in.LoginUseCase;
import com.plantopolis.backend.domain.port.in.RegistrarUsuarioUseCase;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.AuthResponse;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.GoogleLoginRequest;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.LoginRequest;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.RegistroRequest;
import com.plantopolis.backend.infrastructure.security.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(
    name = "Autenticación",
    description = "Registro e inicio de sesión de usuarios. " +
                  "El token JWT obtenido debe usarse en el header: " +
                  "Authorization: Bearer {token}"
)
public class AuthController {

    private final RegistrarUsuarioUseCase registrarUseCase;
    private final LoginUseCase            loginUseCase;
    private final LoginGoogleUseCase      loginGoogleUseCase;
    private final PasswordEncoder         passwordEncoder;

    //  Inyectamos JwtUtil para extraer el rol del token
    private final JwtUtil jwtUtil;

    // ── REGISTRO ─────────────────────────────────────────────────────────
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
            description = "Datos inválidos"
        )
    })
    @PostMapping("/registro")
    public ResponseEntity<AuthResponse> registro(
            @Valid @RequestBody RegistroRequest request) {

        // Registrar el usuario (devuelve el usuario guardado con su ID)
        var usuario = registrarUseCase.registrar(
                request.nombre(),
                request.email(),
                request.password(),
                request.telefono(),
                request.direccion()
        );

        // El registro siempre crea usuarios con rol CLIENTE
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new AuthResponse(
                        null,
                        usuario.getCorreo(),
                        "CLIENTE",
                        "Usuario registrado exitosamente"
                ));
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────
    @Operation(
        summary = "Iniciar sesión",
        description = "Autentica un usuario y devuelve un token JWT válido por 24 horas. " +
                      "**El campo `rol` de la respuesta indica si es CLIENTE o ADMINISTRADOR.** " +
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
                          "email": "admin@plantopolis.com",
                          "rol": "ADMINISTRADOR",
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

        // ── HONEYPOT: trampa anti-bot ────────────────────────────────────
        // El campo "website" es invisible para usuarios reales (hidden en el form).
        // Si llega con valor, un bot llenó el formulario → rechazar silenciosamente
        // con un 401 genérico sin tocar la BD ni el AuthenticationManager.
        if (request.website() != null && !request.website().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(null, null, null, "Credenciales incorrectas"));
        }

        // PASO 1: Autenticar y generar el JWT
        // loginUseCase.login() verifica la contraseña con BCrypt y genera
        // un token con el claim "rol" (ej: "ADMINISTRADOR") dentro del payload
        String token = loginUseCase.login(request.email(), request.password());

        // PASO 2: ✅ [FIX] Extraer el rol del JWT recién generado
        // JwtUtil.extraerRol() lee el claim "rol" del payload del token.
        // Antes esta línea no existía y se devolvía null → el frontend
        // no podía detectar el rol ADMINISTRADOR → el panel admin no aparecía.
        String rol = jwtUtil.extraerRol(token);

        ResponseCookie jwtCookie = ResponseCookie.from("jwt_token", token)
                .httpOnly(true)
                .secure(false) // Debería ser true en producción con HTTPS
                .path("/")
                .maxAge(24 * 60 * 60) // 24 horas
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(new AuthResponse(
                        null, // Ya no enviamos el token en el body por seguridad
                        request.email(),
                        rol,
                        "Login exitoso"
                ));
    }

    // ── LOGIN GOOGLE ──────────────────────────────────────────────────────
    @Operation(
        summary = "Iniciar sesión / registrarse con Google",
        description = "Recibe el ID Token emitido por Google, lo valida, registra o autentica al usuario y establece la cookie de sesión JWT."
    )
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        var result = loginGoogleUseCase.loginConGoogle(request.idToken());

        ResponseCookie jwtCookie = ResponseCookie.from("jwt_token", result.token())
                .httpOnly(true)
                .secure(false) // Debería ser true en producción con HTTPS
                .path("/")
                .maxAge(24 * 60 * 60) // 24 horas
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(new AuthResponse(
                        null,
                        result.email(),
                        result.rol(),
                        "Autenticación con Google exitosa"
                ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie clearCookie = ResponseCookie.from("jwt_token", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0) // Borra la cookie
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .build();
    }
}