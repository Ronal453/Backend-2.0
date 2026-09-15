package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.Usuario;
import com.plantopolis.backend.domain.port.in.GestionarUsuariosAdminUseCase;
import com.plantopolis.backend.domain.port.out.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class AdminUsuarioService implements GestionarUsuariosAdminUseCase {

    private final UsuarioRepositoryPort usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String ALFABETO_PASSWORD =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int LONGITUD_PASSWORD_TEMPORAL = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    // IDs de rol según los INSERT de Rol en plantopolis_schema.sql
    private static final Long ROL_TRABAJADOR = 2L;

    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");

    @Override
    public Page<Usuario> listarUsuarios(String nombre, Long idRol, Boolean activo, Pageable pageable) {
        return usuarioRepository.listarTodos(nombre, idRol, activo, pageable);
    }

    @Override
    @Transactional
    public Usuario activar(Long idUsuario) {
        var usuario = obtenerOFallar(idUsuario);
        usuario.setActivo(true);
        return usuarioRepository.guardar(usuario);
    }

    @Override
    @Transactional
    public Usuario desactivar(Long idUsuario) {
        var usuario = obtenerOFallar(idUsuario);
        usuario.setActivo(false);
        return usuarioRepository.guardar(usuario);
    }

    @Override
    @Transactional
    public String resetearPassword(Long idUsuario) {
        var usuario = obtenerOFallar(idUsuario);
        String passwordTemporal = generarPasswordTemporal();
        usuario.setContrasenaHash(passwordEncoder.encode(passwordTemporal));
        usuarioRepository.guardar(usuario);
        return passwordTemporal;
    }

    // ── NUEVO: creación de cuentas TRABAJADOR por el admin ──────────────────
    @Override
    @Transactional
    public Usuario crearTrabajador(String nombreCompleto, String correo, String passwordInicial) {

        if (usuarioRepository.existePorEmail(correo)) {
            throw new RuntimeException("El email ya está registrado: " + correo);
        }

        Usuario nuevo = Usuario.builder()
                .nombreCompleto(nombreCompleto)
                .correo(correo)
                .contrasenaHash(passwordEncoder.encode(passwordInicial))
                .idRol(ROL_TRABAJADOR)
                .activo(true)
                .fechaRegistro(LocalDateTime.now(ZONA_BOGOTA))
                .fechaActualizacion(LocalDateTime.now(ZONA_BOGOTA))
                .build();

        return usuarioRepository.guardar(nuevo);
    }

    private Usuario obtenerOFallar(Long idUsuario) {
        return usuarioRepository.buscarPorId(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + idUsuario));
    }

    private String generarPasswordTemporal() {
        StringBuilder sb = new StringBuilder(LONGITUD_PASSWORD_TEMPORAL);
        for (int i = 0; i < LONGITUD_PASSWORD_TEMPORAL; i++) {
            sb.append(ALFABETO_PASSWORD.charAt(RANDOM.nextInt(ALFABETO_PASSWORD.length())));
        }
        return sb.toString();
    }
}