package com.plantopolis.backend.infrastructure.adapter.in.web;

import com.plantopolis.backend.domain.port.in.LoginUseCase;
import com.plantopolis.backend.domain.port.in.RegistrarUsuarioUseCase;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.AuthResponse;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.LoginRequest;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.RegistroRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegistrarUsuarioUseCase registrarUseCase;
    private final LoginUseCase loginUseCase;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/registro")
    public ResponseEntity<AuthResponse> registro(
            @Valid @RequestBody RegistroRequest request) {
        var usuario = registrarUseCase.registrar(
                request.nombre(), request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(null, usuario.getCorreo(),
                        "CLIENTE", "Usuario registrado exitosamente"));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        String token = loginUseCase.login(request.email(), request.password());
        return ResponseEntity.ok(new AuthResponse(
                token, request.email(), null, "Login exitoso"));
    }

    // ⚠️ SOLO DESARROLLO - eliminar antes de producción
    @GetMapping("/hash")
    public String generarHash(@RequestParam String password) {
        return passwordEncoder.encode(password);
    }
}
