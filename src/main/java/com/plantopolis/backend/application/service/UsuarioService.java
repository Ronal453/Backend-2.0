package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.Usuario;
import com.plantopolis.backend.domain.port.in.LoginUseCase;
import com.plantopolis.backend.domain.port.in.RegistrarUsuarioUseCase;
import com.plantopolis.backend.domain.port.out.UsuarioRepositoryPort;
import com.plantopolis.backend.infrastructure.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId; // ← importar ZoneId para manejar zonas horarias

@Service
@RequiredArgsConstructor
public class UsuarioService implements RegistrarUsuarioUseCase, LoginUseCase {

    private final UsuarioRepositoryPort usuarioRepository;
    private final PasswordEncoder       passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtUtil               jwtUtil;
    private final UserDetailsService    userDetailsService;

    // ID del rol CLIENTE según la tabla ROL en Oracle (INSERT inicial del SQL)
    private static final Long ROL_CLIENTE = 1L;

    // Zona horaria de Colombia — UTC-5, sin horario de verano
    // Usada para que la fecha de registro quede en hora de Bogotá,
    // aunque el servidor (Docker) corra en UTC
    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");

    @Override
    public Usuario registrar(String nombre, String email, String password,
                             String telefono, String direccion) {

        // Verificar que el email no esté ya registrado en la BD
        if (usuarioRepository.existePorEmail(email)) {
            throw new RuntimeException("El email ya está registrado: " + email);
        }

        Usuario nuevo = Usuario.builder()
                .nombreCompleto(nombre)
                .correo(email)
                // Hashear la contraseña con BCrypt antes de guardar
                .contrasenaHash(passwordEncoder.encode(password))
                .idRol(ROL_CLIENTE)
                .telefono(telefono)
                .direccion(direccion)
                // ── FIX TIMEZONE ──────────────────────────────────────────
                // LocalDateTime.now() sin parámetro usa la zona del servidor.
                // En Docker el servidor corre en UTC → la hora queda 5h adelante.
                // LocalDateTime.now(ZONA_BOGOTA) fuerza la hora de Colombia (UTC-5)
                // independientemente de dónde esté desplegado el contenedor.
                .fechaRegistro(LocalDateTime.now(ZONA_BOGOTA))
                .build();

        // Persistir el nuevo usuario en Oracle a través del puerto de salida
        return usuarioRepository.guardar(nuevo);
    }

    @Override
    public String login(String email, String password) {
        // Delegar la autenticación a Spring Security (valida email + password con BCrypt)
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password));

        // Cargar los detalles del usuario para generar el token
        var userDetails = userDetailsService.loadUserByUsername(email);

        // Obtener el rol del usuario desde el repositorio
        var usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Si el rol es null por algún motivo, asignar CLIENTE como fallback
        String rol = (usuario.getRolNombre() != null)
                ? usuario.getRolNombre()
                : "CLIENTE";

        // Generar y devolver el JWT firmado con el email y el rol
        return jwtUtil.generarToken(userDetails, rol);
    }
}