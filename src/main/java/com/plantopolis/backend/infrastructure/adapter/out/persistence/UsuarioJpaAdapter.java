package com.plantopolis.backend.infrastructure.adapter.out.persistence;

import com.plantopolis.backend.domain.model.Usuario;
import com.plantopolis.backend.domain.port.out.UsuarioRepositoryPort;
import com.plantopolis.backend.infrastructure.persistence.mapper.UsuarioMapper;
import com.plantopolis.backend.infrastructure.persistence.repository.UsuarioJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UsuarioJpaAdapter implements UsuarioRepositoryPort {

    private final UsuarioJpaRepository jpaRepository;
    private final UsuarioMapper mapper;

    @Override
    public Usuario guardar(Usuario usuario) {
        var entity = mapper.toEntity(usuario);
        var saved  = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return jpaRepository.findByCorreo(email)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existePorEmail(String email) {
        return jpaRepository.existsByCorreo(email);
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Page<Usuario> listarTodos(String nombre, Long idRol, Boolean activo, Pageable pageable) {
        return jpaRepository.buscarConFiltros(nombre, idRol, activo, pageable)
                .map(mapper::toDomain);
    }
}