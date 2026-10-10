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

        Long idRolCliente = usuarioRepository.obtenerIdRol("CLIENTE");

        Usuario nuevo = Usuario.builder()
                .nombreCompleto(nombre)
                .correo(email)
                .contrasenaHash(passwordEncoder.encode(password))
                .idRol(idRolCliente)
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

        if (usuario.getUltimoToken() != null && jwtUtil.estaVigente(usuario.getUltimoToken())) {
            throw new RuntimeException("Ya se encuentra una sesión activa. Por favor, espere un tiempo a que expire o cierre la sesión activa.");
        }

        String token = jwtUtil.generarToken(userDetails, rol);
        usuario.setUltimoToken(token);
        usuarioRepository.guardar(usuario);
        return token;
    }

    private GoogleIdToken.Payload verificarGoogleToken(String googleIdToken) {
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

            return idToken.getPayload();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al verificar token de Google: {}", e.getMessage(), e);
            throw new RuntimeException("Error al autenticar con Google: " + e.getMessage());
        }
    }

    @Override
    public LoginGoogleResult loginConGoogle(String googleIdToken) {
        GoogleIdToken.Payload payload = verificarGoogleToken(googleIdToken);
        String email = payload.getEmail();
        String nombre = (String) payload.get("name");

        if (email == null || email.isBlank()) {
            throw new RuntimeException("El token de Google no contiene un email válido");
        }

        // Buscar si el usuario ya existe, sino crearlo con ROL_CLIENTE
        Usuario usuario = usuarioRepository.buscarPorEmail(email).orElseGet(() -> {
            log.info("Creando nuevo usuario vía Google Sign-In para email: {}", email);
            String randomPassword = UUID.randomUUID().toString();
            Long idRolCliente = usuarioRepository.obtenerIdRol("CLIENTE");
            
            Usuario nuevo = Usuario.builder()
                    .nombreCompleto(nombre != null ? nombre : email)
                    .correo(email)
                    .contrasenaHash(passwordEncoder.encode(randomPassword))
                    .idRol(idRolCliente)
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
        
        if (usuario.getUltimoToken() != null && jwtUtil.estaVigente(usuario.getUltimoToken())) {
            throw new RuntimeException("Ya se encuentra una sesión activa. Por favor, espere un tiempo a que expire o cierre la sesión activa.");
        }
        
        String token = jwtUtil.generarToken(userDetails, rol);

        usuario.setUltimoToken(token);
        usuarioRepository.guardar(usuario);

        return new LoginGoogleResult(token, email, rol);
    }

    @Override
    public LoginGoogleResult registrarConGoogle(String googleIdToken) {
        GoogleIdToken.Payload payload = verificarGoogleToken(googleIdToken);
        String email = payload.getEmail();
        String nombre = (String) payload.get("name");

        if (email == null || email.isBlank()) {
            throw new RuntimeException("El token de Google no contiene un email válido");
        }

        // Si la cuenta YA existe, lanzar excepción impidiendo volver a registrarla
        if (usuarioRepository.existePorEmail(email)) {
            throw new RuntimeException("El email ya está registrado: " + email);
        }

        log.info("Registrando nuevo usuario vía Google Sign-In para email: {}", email);
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
        Usuario guardado = usuarioRepository.guardar(nuevo);

        var userDetails = userDetailsService.loadUserByUsername(email);
        String rol = (guardado.getRolNombre() != null) ? guardado.getRolNombre() : "CLIENTE";
        String token = jwtUtil.generarToken(userDetails, rol);

        guardado.setUltimoToken(token);
        usuarioRepository.guardar(guardado);

        return new LoginGoogleResult(token, email, rol);
    }
}