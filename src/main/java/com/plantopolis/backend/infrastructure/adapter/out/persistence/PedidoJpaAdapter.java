package com.plantopolis.backend.infrastructure.adapter.out.persistence;

import com.plantopolis.backend.domain.model.Pedido;
import com.plantopolis.backend.domain.port.out.PedidoRepositoryPort;
import com.plantopolis.backend.infrastructure.persistence.mapper.PedidoMapper;
import com.plantopolis.backend.infrastructure.persistence.repository.PedidoJpaRepository;
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
    private final PedidoMapper mapper;

    @Override
    public Optional<Pedido> buscarPorId(Long idPedido) {
        return pedidoRepo.findById(idPedido)
                .map(mapper::toDomain);
    }

    @Override
    public List<Pedido> buscarPorUsuario(Long idUsuario) {
        return pedidoRepo
                .findByIdUsuarioOrderByFechaPedidoDesc(idUsuario)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public String generarNumeroPedido() {
        String fecha = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        String numero;
        int intentos = 0;
        do {
            if (intentos++ > 10) {
                throw new RuntimeException(
                        "No se pudo generar un número de pedido único");
            }
            String rand = String.valueOf(
                    ThreadLocalRandom.current().nextInt(1000, 9999));
            numero = "PL-" + fecha + "-" + rand;
        } while (pedidoRepo.existsByNumeroPedido(numero));

        return numero;
    }
}