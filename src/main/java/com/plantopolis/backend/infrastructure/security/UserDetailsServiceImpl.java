package com.plantopolis.backend.infrastructure.security;

import com.plantopolis.backend.domain.port.out.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepositoryPort usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        var usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado: " + email));

        // Si rolNombre es null, asignar CLIENTE por defecto
        String rol = (usuario.getRolNombre() != null)
                ? usuario.getRolNombre()
                : "CLIENTE";

        return User.builder()
                .username(usuario.getCorreo())
                .password(usuario.getContrasenaHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + rol)))
                .build();
    }
}