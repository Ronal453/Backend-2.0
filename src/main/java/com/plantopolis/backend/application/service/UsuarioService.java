package com.plantopolis.backend.application.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.plantopolis.backend.domain.model.Usuario;
import com.plantopolis.backend.domain.port.in.LoginGoogleUseCase;
import com.plantopolis.backend.domain.port.in.LoginUseCase;
import com.plantopolis.backend.domain.port.in.RecuperarPasswordUseCase;
import com.plantopolis.backend.domain.port.in.RegistrarUsuarioUseCase;
import com.plantopolis.backend.domain.port.out.NotificacionPort;
import com.plantopolis.backend.domain.port.out.UsuarioRepositoryPort;
import com.plantopolis.backend.infrastructure.config.GoogleOAuthConfig;
import com.plantopolis.backend.infrastructure.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService implements RegistrarUsuarioUseCase, LoginUseCase, LoginGoogleUseCase, RecuperarPasswordUseCase {

    private final UsuarioRepositoryPort usuarioRepository;
    private final PasswordEncoder       passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtUtil               jwtUtil;
    private final UserDetailsService    userDetailsService;
    private final GoogleOAuthConfig     googleOAuthConfig;
    private final NotificacionPort      notificacionPort;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    private static final Long ROL_CLIENTE = 3L;
    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");

    @Override
    public Usuario registrar(String nombre, String email, String password,
                             String telefono, String direccion) {

        if (usuarioRepository.existePorEmail(email)) {
            throw new RuntimeException("El email ya está registrado: " + email);
        }

        Long idRolCliente = usuarioRepository.obtenerIdRol("CLIENTE");
        if (idRolCliente == null || idRolCliente == 0L) {
            idRolCliente = ROL_CLIENTE;
        }

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

    /**
     * Autentica a un usuario mediante credenciales tradicionales (correo y contraseña).
     * Emite un nuevo token JWT de 24 horas y actualiza 'ultimoToken' en la base de datos.
     * Al iniciar sesión con credenciales válidas, cualquier sesión previa en otro dispositivo
     * queda automáticamente superada e invalidada por JwtAuthenticationFilter.
     *
     * @param email Correo electrónico registrado del usuario
     * @param password Contraseña en texto claro para validación BCrypt
     * @return Token JWT firmado con los claims del usuario
     */
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

    /**
     * Procesa la solicitud de recuperación de contraseña de un usuario (HU2).
     * Si la cuenta existe y está activa, emite un token JWT de 15 minutos y envía el correo.
     * Si no existe o está desactivada, no arroja excepción para prevenir enumeración de usuarios (OWASP).
     *
     * @param email Correo electrónico suministrado en el formulario
     */
    @Override
    public void solicitarRecuperacion(String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        usuarioRepository.buscarPorEmail(email.trim().toLowerCase()).ifPresent(usuario -> {
            if (Boolean.TRUE.equals(usuario.getActivo())) {
                String token = jwtUtil.generarTokenRecuperacion(usuario.getCorreo());
                String enlace = frontendUrl + "/resetear-password?token=" + token;
                log.info("Generado token de recuperación para usuario {}. Enviando notificación...", usuario.getCorreo());
                notificacionPort.enviarRecuperacionPassword(usuario.getCorreo(), usuario.getNombreCompleto(), enlace);
            } else {
                log.warn("Solicitud de recuperación ignorada para usuario inactivo: {}", email);
            }
        });
    }

    /**
     * Valida el token temporal y restablece la contraseña del usuario (HU2).
     * Invalida cualquier sesión previa o token previo actualizando 'fechaActualizacion' y limpiando 'ultimoToken'.
     *
     * @param token Token JWT temporal con propósito RECUPERACION_PASSWORD
     * @param nuevaPassword Nueva contraseña sin cifrar (validada para cumplir con la longitud mínima)
     */
    @Override
    @Transactional
    public void restablecerPassword(String token, String nuevaPassword) {
        if (token == null || !jwtUtil.validarTokenRecuperacion(token)) {
            throw new IllegalArgumentException("El enlace de recuperación es inválido o ha expirado. Por favor solicita uno nuevo.");
        }

        if (nuevaPassword == null || !nuevaPassword.trim().matches("^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$")) {
            throw new IllegalArgumentException("La contraseña debe tener mínimo 8 caracteres, al menos una mayúscula, un número y un carácter especial (@$!%*?&).");
        }

        String email = jwtUtil.extraerEmail(token);
        Usuario usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new IllegalStateException("La cuenta está desactivada. No es posible cambiar la contraseña.");
        }

        // Cifrar la nueva contraseña con BCrypt
        usuario.setContrasenaHash(passwordEncoder.encode(nuevaPassword.trim()));
        // Invalida sesiones previas forzando re-autenticación
        usuario.setUltimoToken(null);
        usuario.setFechaActualizacion(LocalDateTime.now(ZONA_BOGOTA));

        usuarioRepository.guardar(usuario);
        log.info("Contraseña restablecida exitosamente para el usuario: {}", email);
    }
}