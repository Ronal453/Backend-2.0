package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.Usuario;
import com.plantopolis.backend.domain.port.in.RegistrarUsuarioUseCase;
import com.plantopolis.backend.domain.port.out.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioService implements RegistrarUsuarioUseCase {

    private final UsuarioRepositoryPort usuarioRepository;

    @Override
    public Usuario registrar(String nombre, String email, String password) {
        if (usuarioRepository.existePorEmail(email)) {
            throw new RuntimeException("El email ya está registrado");
        }
        // lógica de negocio aquí...
        Usuario usuario = new Usuario();
            usuario.setNombre(nombre);
            usuario.setEmail(email);
            usuario.setPassword(password);
        return usuarioRepository.guardar(usuario);
    }
}