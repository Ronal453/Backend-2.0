package com.plantopolis.backend.infrastructure.persistence.repository;

import com.plantopolis.backend.infrastructure.persistence.entity.PedidoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PedidoJpaRepository
        extends JpaRepository<PedidoEntity, Long> {

    List<PedidoEntity> findByIdUsuarioOrderByFechaPedidoDesc(Long idUsuario);

    boolean existsByNumeroPedido(String numeroPedido);
}