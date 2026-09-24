package com.plantopolis.backend.application.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.plantopolis.backend.domain.model.Usuario;
import com.plantopolis.backend.domain.port.in.LoginGoogleUseCase;
import com.plantopolis.backend.domain.port.in.LoginUseCase;
import com.plantopolis.backend.domain.port.in.RegistrarUsuarioUseCase;
import com.plantopolis.backend.domain.port.out.UsuarioRepositoryPort;
import com.plantopolis.backend.infrastructure.config.GoogleOAuthConfig;
import com.plantopolis.backend.infrastructure.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService implements RegistrarUsuarioUseCase, LoginUseCase, LoginGoogleUseCase {

    private final UsuarioRepositoryPort usuarioRepository;
    private final PasswordEncoder       passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtUtil               jwtUtil;
    private final UserDetailsService    userDetailsService;
    private final GoogleOAuthConfig     googleOAuthConfig;

    private static final Long ROL_CLIENTE = 3L;
    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");

    @Override
    public Usuario registrar(String nombre, String email, String password,
                             String telefono, String direccion) {

        if (usuarioRepository.existePorEmail(email)) {
            throw new RuntimeException("El email ya está registrado: " + email);
        }

        Usuario nuevo = Usuario.builder()
                .nombreCompleto(nombre)
                .correo(email)
                .contrasenaHash(passwordEncoder.encode(password))
                .idRol(ROL_CLIENTE)
                .telefono(telefono)
                .direccion(direccion)
                .fechaRegistro(LocalDateTime.now(ZONA_BOGOTA))
                .activo(true)
                .fechaActualizacion(LocalDateTime.now(ZONA_BOGOTA))
                .build();

        return usuarioRepository.guardar(nuevo);
    }

    @Override
    public String login(String email, String password) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password));

        var userDetails = userDetailsService.loadUserByUsername(email);

        var usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String rol = (usuario.getRolNombre() != null)
                ? usuario.getRolNombre()
                : "CLIENTE";

        return jwtUtil.generarToken(userDetails, rol);
    }

    @Override
    public LoginGoogleResult loginConGoogle(String googleIdToken) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            )
            .setAudience(Collections.singletonList(googleOAuthConfig.getClientId()))
            .build();

            GoogleIdToken idToken = verifier.verify(googleIdToken);
            if (idToken == null) {
                throw new RuntimeException("El token de Google es inválido o ha expirado");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String nombre = (String) payload.get("name");

            if (email == null || email.isBlank()) {
                throw new RuntimeException("El token de Google no contiene un email válido");
            }

            // Buscar si el usuario ya existe, sino crearlo con ROL_CLIENTE
            Usuario usuario = usuarioRepository.buscarPorEmail(email).orElseGet(() -> {
                log.info("Creando nuevo usuario vía Google Sign-In para email: {}", email);
                String randomPassword = UUID.randomUUID().toString();
                Usuario nuevo = Usuario.builder()
                        .nombreCompleto(nombre != null ? nombre : email)
                        .correo(email)
                        .contrasenaHash(passwordEncoder.encode(randomPassword))
                        .idRol(ROL_CLIENTE)
                        .fechaRegistro(LocalDateTime.now(ZONA_BOGOTA))
                        .activo(true)
                        .fechaActualizacion(LocalDateTime.now(ZONA_BOGOTA))
                        .build();
                return usuarioRepository.guardar(nuevo);
            });

            if (Boolean.FALSE.equals(usuario.getActivo())) {
                throw new RuntimeException("La cuenta está desactivada. Contacte al administrador.");
            }

            var userDetails = userDetailsService.loadUserByUsername(email);
            String rol = (usuario.getRolNombre() != null) ? usuario.getRolNombre() : "CLIENTE";
            String token = jwtUtil.generarToken(userDetails, rol);

            return new LoginGoogleResult(token, email, rol);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al verificar token de Google: {}", e.getMessage(), e);
            throw new RuntimeException("Error al autenticar con Google: " + e.getMessage());
        }
    }
}