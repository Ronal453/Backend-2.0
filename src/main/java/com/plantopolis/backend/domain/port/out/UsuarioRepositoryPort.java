package com.plantopolis.backend.domain.port.out;

import com.plantopolis.backend.domain.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UsuarioRepositoryPort {
    Usuario guardar(Usuario usuario);
    Optional<Usuario> buscarPorEmail(String email);
    boolean existePorEmail(String email);

    Optional<Usuario> buscarPorId(Long id);

    /**
     * Lista usuarios con filtros combinables (todos opcionales).
     * Sigue el mismo patrón que ProductoRepositoryPort.buscarTodosAdmin().
     */
    Page<Usuario> listarTodos(String nombre, Long idRol, Boolean activo, Pageable pageable);
}