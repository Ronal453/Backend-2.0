package com.plantopolis.backend.infrastructure.adapter.out.persistence;

import com.plantopolis.backend.domain.model.DetallePedido;
import com.plantopolis.backend.domain.model.Pago;
import com.plantopolis.backend.domain.model.Pedido;
import com.plantopolis.backend.domain.port.out.PedidoRepositoryPort;
import com.plantopolis.backend.infrastructure.persistence.entity.DetallePedidoEntity;
import com.plantopolis.backend.infrastructure.persistence.entity.PagoEntity;
import com.plantopolis.backend.infrastructure.persistence.entity.PedidoEntity;
import com.plantopolis.backend.infrastructure.persistence.mapper.PedidoMapper;
import com.plantopolis.backend.infrastructure.persistence.repository.DetallePedidoJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.repository.EstadoPedidoJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.repository.MetodoPagoJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.repository.PagoJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.repository.PedidoJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PedidoJpaAdapter implements PedidoRepositoryPort {

    private final PedidoJpaRepository pedidoRepo;
    private final DetallePedidoJpaRepository detallePedidoRepo;
    private final PagoJpaRepository pagoRepo;
    private final MetodoPagoJpaRepository metodoPagoRepo;
    private final EstadoPedidoJpaRepository estadoPedidoRepo;
    private final PedidoMapper mapper;

    @PersistenceContext
    private EntityManager entityManager;

    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");
    private static final Long ESTADO_PEDIDO_PENDIENTE = 1L;
    private static final Long ESTADO_PAGO_APROBADO = 2L;
    private static final Long ID_METODO_EFECTIVO = 4L;

    @Override
    public Optional<Pedido> buscarPorId(Long idPedido) {
        entityManager.clear();
        return pedidoRepo.findByIdWithRelations(idPedido).map(this::mapAndEnrich);
    }

    @Override
    public List<Pedido> buscarPorUsuario(Long idUsuario) {
        return pedidoRepo.findByIdUsuarioWithRelations(idUsuario)
                .stream().map(this::mapAndEnrich).toList();
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
                .stream().map(this::mapAndEnrich).toList();
    }

    @Override
    public Pedido procesarNuevoPedido(Pedido pedido, Long idMetodoPago) {
        // 1. Guardar Pedido
        PedidoEntity pedidoEntity = PedidoEntity.builder()
                .idUsuario(pedido.getIdUsuario())
                .idEstado(ESTADO_PEDIDO_PENDIENTE)
                .fechaPedido(pedido.getFechaPedido())
                .direccionEnvio(pedido.getDireccionEnvio())
                .numeroPedido(pedido.getNumeroPedido())
                .subtotal(pedido.getSubtotal())
                .impuestos(pedido.getImpuestos())
                .total(pedido.getTotal())
                .build();

        PedidoEntity savedPedido = pedidoRepo.saveAndFlush(pedidoEntity);
        Long idPedidoGenerado = savedPedido.getIdPedido();

        // 2. Guardar Detalles
        List<DetallePedidoEntity> detalleEntities = pedido.getDetalles().stream().map(d ->
                DetallePedidoEntity.builder()
                        .idPedido(idPedidoGenerado)
                        .idProducto(d.getIdProducto())
                        .cantidad(d.getCantidad())
                        .precioUnitario(d.getPrecioUnitario())
                        .subtotal(d.getSubtotal())
                        .build()
        ).toList();
        detallePedidoRepo.saveAll(detalleEntities);

        // 3. Guardar Pago
        PagoEntity pagoEntity = PagoEntity.builder()
                .idPedido(idPedidoGenerado)
                .idMetodo(idMetodoPago)
                .idEstadoPago(ESTADO_PAGO_APROBADO)
                .monto(pedido.getTotal())
                .fechaPago(LocalDateTime.now(ZONA_BOGOTA))
                .build();
        pagoRepo.save(pagoEntity);

        entityManager.clear();
        return buscarPorIdEnriquecido(idPedidoGenerado);
    }

    @Override
    public Page<Pedido> buscarTodosAdmin(String estadoFiltro, Pageable pageable) {
        return pedidoRepo.findAllAdminWithFilters(estadoFiltro, pageable)
                .map(this::mapAndEnrich);
    }

    @Override
    public Pedido actualizarEstadoPorDescripcion(Long idPedido, String nuevoEstado) {
        var pedidoEntity = pedidoRepo.findById(idPedido)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado: " + idPedido));

        var nuevoEstadoEntity = estadoPedidoRepo.findByDescripcionEstado(nuevoEstado)
                .orElseThrow(() -> new RuntimeException("Estado no encontrado en BD: " + nuevoEstado));

        pedidoEntity.setIdEstado(nuevoEstadoEntity.getIdEstadoPedido());
        pedidoRepo.saveAndFlush(pedidoEntity);
        entityManager.clear();

        return buscarPorIdEnriquecido(idPedido);
    }

    @Override
    public Pedido buscarPorIdEnriquecido(Long idPedido) {
        return buscarPorId(idPedido)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado: " + idPedido));
    }

    /**
     * Mapea la entidad a Dominio y le enriquece el objeto Pago.
     * Centralizamos esta lógica aquí para no romper la arquitectura en los servicios.
     */
    private Pedido mapAndEnrich(PedidoEntity entity) {
        Pedido pedido = mapper.toDomain(entity);

        var pagoEntityOpt = pagoRepo.findByIdPedido(pedido.getIdPedido());
        if (pagoEntityOpt.isEmpty()) {
            return pedido;
        }

        PagoEntity pagoEntity = pagoEntityOpt.get();
        Long idMetodo = pagoEntity.getIdMetodo();
        String nombreMetodo = "DESCONOCIDO";
        if (idMetodo != null) {
            nombreMetodo = metodoPagoRepo.findById(idMetodo)
                    .map(m -> m.getNombreMetodo())
                    .orElse("DESCONOCIDO");
        }

        String estadoPago = ID_METODO_EFECTIVO.equals(idMetodo) ? "PENDIENTE" : "APROBADO";

        if (pedido.getPago() == null) {
            pedido.setPago(Pago.builder()
                    .monto(pagoEntity.getMonto())
                    .fechaPago(pagoEntity.getFechaPago())
                    .nombreMetodo(nombreMetodo)
                    .estadoPagoDescripcion(estadoPago)
                    .idMetodo(idMetodo)
                    .build());
        } else {
            pedido.getPago().setNombreMetodo(nombreMetodo);
            pedido.getPago().setEstadoPagoDescripcion(estadoPago);
            pedido.getPago().setIdMetodo(idMetodo);
            if (pedido.getPago().getMonto() == null) {
                pedido.getPago().setMonto(pagoEntity.getMonto());
            }
        }

        return pedido;
    }
}