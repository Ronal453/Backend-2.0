package com.plantopolis.backend.infrastructure.adapter.out.persistence;

import com.plantopolis.backend.domain.model.Pedido;
import com.plantopolis.backend.domain.port.out.PedidoRepositoryPort;
import com.plantopolis.backend.infrastructure.persistence.mapper.PedidoMapper;
import com.plantopolis.backend.infrastructure.persistence.repository.PagoJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.repository.PedidoJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class PedidoJpaAdapter implements PedidoRepositoryPort {

    private final PedidoJpaRepository pedidoRepo;
    private final PagoJpaRepository pagoRepo;
    private final PedidoMapper mapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Pedido> buscarPorId(Long idPedido) {
        entityManager.clear();
        return pedidoRepo.findByIdWithRelations(idPedido).map(mapper::toDomain);
    }

    @Override
    public List<Pedido> buscarPorUsuario(Long idUsuario) {
        return pedidoRepo.findByIdUsuarioWithRelations(idUsuario)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public String generarNumeroPedido() {
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String numero;
        int intentos = 0;
        do {
            if (intentos++ > 10) {
                throw new RuntimeException("No se pudo generar número de pedido único");
            }
            String rand = String.valueOf(ThreadLocalRandom.current().nextInt(1000, 9999));
            numero = "PL-" + fecha + "-" + rand;
        } while (pedidoRepo.existsByNumeroPedido(numero));
        return numero;
    }

    @Override
    public List<Pedido> buscarPorRangoFecha(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return pedidoRepo.buscarPorRangoFecha(fechaInicio, fechaFin)
                .stream().map(mapper::toDomain).toList();
    }
}