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

@Service
@RequiredArgsConstructor
public class UsuarioService implements RegistrarUsuarioUseCase, LoginUseCase {

    private final UsuarioRepositoryPort usuarioRepository;
    private final PasswordEncoder       passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtUtil               jwtUtil;
    private final UserDetailsService    userDetailsService;

    private static final Long ROL_CLIENTE = 1L;

    @Override
    public Usuario registrar(String nombre, String email, String password) {
        if (usuarioRepository.existePorEmail(email)) {
            throw new RuntimeException("El email ya está registrado: " + email);
        }
        Usuario nuevo = Usuario.builder()
                .nombreCompleto(nombre)
                .correo(email)
                .contrasenaHash(passwordEncoder.encode(password))
                .idRol(ROL_CLIENTE)
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
}
