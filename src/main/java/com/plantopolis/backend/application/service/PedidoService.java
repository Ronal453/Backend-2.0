package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.Pedido;
import com.plantopolis.backend.domain.port.in.ProcesarPedidoUseCase;
import com.plantopolis.backend.domain.port.out.*;
import com.plantopolis.backend.infrastructure.persistence.entity.DetallePedidoEntity;
import com.plantopolis.backend.infrastructure.persistence.entity.PagoEntity;
import com.plantopolis.backend.infrastructure.persistence.entity.PedidoEntity;
import com.plantopolis.backend.infrastructure.persistence.mapper.PedidoMapper;
import com.plantopolis.backend.infrastructure.persistence.repository.MetodoPagoJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.repository.PedidoJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoService implements ProcesarPedidoUseCase {

    // ── Ports (interfaces del dominio) ───────────────────────
    private final UsuarioRepositoryPort     usuarioRepository;
    private final CarritoRepositoryPort     carritoRepository;
    private final ProductoRepositoryPort    productoRepository;
    private final PedidoRepositoryPort      pedidoRepository;
    private final NotificacionPort          notificacionPort;

    // ── Repos JPA directos (para persistir con cascada) ──────
    private final PedidoJpaRepository       pedidoJpaRepo;
    private final MetodoPagoJpaRepository   metodoPagoRepo;
    private final PedidoMapper              pedidoMapper;

    // ── Constantes de estados (según datos iniciales del SQL) ─
    private static final Long ESTADO_PEDIDO_PENDIENTE   = 1L;
    private static final Long ESTADO_CARRITO_CONVERTIDO = 2L;
    private static final Long ESTADO_PAGO_APROBADO      = 2L;

    // ────────────────────────────────────────────────────────
    @Override
    @Transactional
    public Pedido procesarPedido(String email,
                                  Long idMetodoPago,
                                  String direccionEnvio) {

        // 1. Obtener usuario autenticado
        var usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException(
                        "Usuario no encontrado"));

        // 2. Obtener carrito activo con sus items
        var carrito = carritoRepository
                .buscarCarritoActivo(usuario.getIdUsuario())
                .orElseThrow(() -> new RuntimeException(
                        "No tienes un carrito activo"));

        if (carrito.getItems() == null || carrito.getItems().isEmpty()) {
            throw new RuntimeException("El carrito está vacío");
        }

        // 3. Validar que el método de pago existe
        metodoPagoRepo.findById(idMetodoPago)
                .orElseThrow(() -> new RuntimeException(
                        "Método de pago no válido: " + idMetodoPago));

        // 4. Validar stock y construir detalles
        List<DetallePedidoEntity> detalleEntities = carrito.getItems()
                .stream()
                .map(item -> {
                    var producto = productoRepository
                            .buscarPorId(item.getIdProducto())
                            .orElseThrow(() -> new RuntimeException(
                                    "Producto no encontrado: "
                                    + item.getIdProducto()));

                    if (!Boolean.TRUE.equals(producto.getActivo())) {
                        throw new RuntimeException(
                                "Producto no disponible: "
                                + producto.getNombreProducto());
                    }

                    if (producto.getStock() < item.getCantidad()) {
                        throw new RuntimeException(
                                "Stock insuficiente para '"
                                + producto.getNombreProducto()
                                + "'. Disponible: " + producto.getStock());
                    }

                    return DetallePedidoEntity.builder()
                            .idProducto(item.getIdProducto())
                            .cantidad(item.getCantidad())
                            .precioUnitario(item.getPrecioUnitario())
                            .build();
                })
                .toList();

        // 5. Calcular total
        BigDecimal total = carrito.getTotal();

        // 6. Generar número de pedido único
        String numeroPedido = pedidoRepository.generarNumeroPedido();

        // 7. Construir entidad de pago
        var pagoEntity = PagoEntity.builder()
                .idMetodo(idMetodoPago)
                .idEstadoPago(ESTADO_PAGO_APROBADO)
                .monto(total)
                .fechaPago(LocalDateTime.now())
                .build();

        // 8. Construir entidad de pedido (cascada guarda detalles)
        var pedidoEntity = PedidoEntity.builder()
                .idUsuario(usuario.getIdUsuario())
                .idEstado(ESTADO_PEDIDO_PENDIENTE)
                .idCarrito(carrito.getIdCarrito())
                .fechaPedido(LocalDateTime.now())
                .direccionEnvio(direccionEnvio)
                .numeroPedido(numeroPedido)
                .detalles(detalleEntities)
                .build();

        // 9. Persistir pedido (los detalles se guardan por cascada)
        var savedPedido = pedidoJpaRepo.saveAndFlush(pedidoEntity);

        // 10. Persistir pago con el ID del pedido ya generado
        pagoEntity.setIdPedido(savedPedido.getIdPedido());
        // El pago se guarda por separado porque necesitamos el ID del pedido
        var pagoRepository = pedidoJpaRepo; // reutilizamos flush anterior
        savedPedido.setPago(pagoEntity);
        pedidoJpaRepo.saveAndFlush(savedPedido);

        // 11. Reducir stock de cada producto
        carrito.getItems().forEach(item ->
            productoRepository.buscarPorId(item.getIdProducto())
                    .ifPresent(p -> {
                        p.setStock(p.getStock() - item.getCantidad());
                        productoRepository.guardar(p);
                    })
        );

        // 12. Marcar carrito como CONVERTIDO
        carritoRepository.cambiarEstado(
                carrito.getIdCarrito(), ESTADO_CARRITO_CONVERTIDO);

        // 13. Obtener pedido completo con relaciones cargadas
        var pedidoDomain = pedidoRepository
                .buscarPorId(savedPedido.getIdPedido())
                .orElseThrow();

        // 14. Enviar email de confirmación (falla silenciosamente)
        try {
            notificacionPort.enviarConfirmacionPedido(pedidoDomain);
        } catch (Exception e) {
            log.warn("No se pudo enviar email de confirmación: {}",
                    e.getMessage());
        }

        return pedidoDomain;
    }

    // ────────────────────────────────────────────────────────
    @Override
    public List<Pedido> obtenerHistorial(String email) {
        var usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException(
                        "Usuario no encontrado"));
        return pedidoRepository.buscarPorUsuario(usuario.getIdUsuario());
    }

    // ────────────────────────────────────────────────────────
    @Override
    public Pedido obtenerDetalle(String email, Long idPedido) {
        var usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException(
                        "Usuario no encontrado"));

        var pedido = pedidoRepository.buscarPorId(idPedido)
                .orElseThrow(() -> new RuntimeException(
                        "Pedido no encontrado: " + idPedido));

        // Verificar que el pedido pertenece al usuario
        if (!pedido.getIdUsuario().equals(usuario.getIdUsuario())) {
            throw new RuntimeException(
                    "No tienes permisos para ver este pedido");
        }

        return pedido;
    }
}