package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.DetallePedido;
import com.plantopolis.backend.domain.model.Pago;
import com.plantopolis.backend.domain.model.Pedido;
import com.plantopolis.backend.domain.port.in.ProcesarPedidoUseCase;
import com.plantopolis.backend.domain.port.out.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoService implements ProcesarPedidoUseCase {

    private final UsuarioRepositoryPort  usuarioRepository;
    private final CarritoRepositoryPort  carritoRepository;
    private final ProductoRepositoryPort productoRepository;
    private final PedidoRepositoryPort   pedidoRepository;
    private final NotificacionPort       notificacionPort;

    private static final Long ESTADO_CARRITO_PROCESADO  = 3L;
    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");

    @Override
    @Transactional
    public Pedido procesarPedido(String email, Long idMetodoPago, String direccionEnvio) {
        
        // PASO 1: usuario autenticado
        var usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + email));

        // PASO 2: carrito activo
        var carrito = carritoRepository.buscarCarritoActivo(usuario.getIdUsuario())
                .orElseThrow(() -> new RuntimeException("No tienes un carrito activo"));

        if (carrito.getItems() == null || carrito.getItems().isEmpty()) {
            throw new RuntimeException("El carrito está vacío");
        }

        // PASO 3: validar stock y construir detalles + calcular subtotal/impuestos
        List<DetallePedido> detallesDominio = new ArrayList<>();
        BigDecimal subtotalPedido = BigDecimal.ZERO;
        BigDecimal impuestosPedido = BigDecimal.ZERO;

        for (var item : carrito.getItems()) {
            var producto = productoRepository.buscarPorId(item.getIdProducto())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + item.getIdProducto()));

            if (!Boolean.TRUE.equals(producto.getActivo())) {
                throw new RuntimeException("Producto no disponible: " + producto.getNombreProducto());
            }
            if (producto.getStock() < item.getCantidad()) {
                throw new RuntimeException("Stock insuficiente para '" + producto.getNombreProducto()
                        + "'. Disponible: " + producto.getStock());
            }

            BigDecimal subtotalLinea = item.getPrecioUnitario()
                    .multiply(BigDecimal.valueOf(item.getCantidad()));

            BigDecimal porcentajeIva = producto.getPorcentajeIva() != null
                    ? producto.getPorcentajeIva()
                    : new BigDecimal("19.00");

            BigDecimal impuestoLinea = subtotalLinea
                    .multiply(porcentajeIva)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            subtotalPedido = subtotalPedido.add(subtotalLinea);
            impuestosPedido = impuestosPedido.add(impuestoLinea);

            detallesDominio.add(DetallePedido.builder()
                    .idProducto(item.getIdProducto())
                    .cantidad(item.getCantidad())
                    .precioUnitario(item.getPrecioUnitario())
                    .subtotal(subtotalLinea)
                    .nombreProducto(item.getNombreProducto())
                    .imagenUrl(item.getImagenUrl())
                    .build());
        }

        BigDecimal totalPedido = subtotalPedido.add(impuestosPedido);

        // Construimos el pedido en dominio
        Pedido pedidoAGuardar = Pedido.builder()
                .idUsuario(usuario.getIdUsuario())
                .fechaPedido(LocalDateTime.now(ZONA_BOGOTA))
                .direccionEnvio(direccionEnvio)
                .numeroPedido(pedidoRepository.generarNumeroPedido())
                .subtotal(subtotalPedido)
                .impuestos(impuestosPedido)
                .total(totalPedido)
                .detalles(detallesDominio)
                .build();

        // PASO 4: Guardar todo usando el puerto (atómico en adaptador)
        Pedido pedidoRespuesta = pedidoRepository.procesarNuevoPedido(pedidoAGuardar, idMetodoPago);

        // PASO 5: reducir stock
        carrito.getItems().forEach(item ->
            productoRepository.buscarPorId(item.getIdProducto())
                    .ifPresent(p -> {
                        p.setStock(p.getStock() - item.getCantidad());
                        productoRepository.guardar(p);
                    })
        );

        // PASO 6: marcar carrito como PROCESADO
        carritoRepository.cambiarEstado(carrito.getIdCarrito(), ESTADO_CARRITO_PROCESADO);
        log.debug("Carrito {} → PROCESADO", carrito.getIdCarrito());

        if (pedidoRespuesta.getEmailCliente() == null) {
            pedidoRespuesta.setEmailCliente(usuario.getCorreo());
            pedidoRespuesta.setNombreCliente(usuario.getNombreCompleto());
        }

        // PASO 7: enviar email (falla silenciosamente)
        try {
            notificacionPort.enviarConfirmacionPedido(pedidoRespuesta);
            log.info("Email de confirmación enviado a {}", usuario.getCorreo());
        } catch (Exception e) {
            log.warn("El envío de correo de confirmación falló: {}", e.getMessage());
        }

        return pedidoRespuesta;
    }

    @Override
    public List<Pedido> obtenerHistorial(String email) {
        var usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return pedidoRepository.buscarPorUsuario(usuario.getIdUsuario());
    }

    @Override
    public Pedido obtenerDetalle(String email, Long idPedido) {
        var usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        var pedido = pedidoRepository.buscarPorIdEnriquecido(idPedido);

        if (!pedido.getIdUsuario().equals(usuario.getIdUsuario())) {
            throw new RuntimeException("No tienes permisos para ver este pedido");
        }
        return pedido;
    }
}