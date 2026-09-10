package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.DetallePedido;
import com.plantopolis.backend.domain.model.Pago;
import com.plantopolis.backend.domain.model.Pedido;
import com.plantopolis.backend.domain.port.in.ProcesarPedidoUseCase;
import com.plantopolis.backend.domain.port.out.*;
import com.plantopolis.backend.infrastructure.persistence.entity.DetallePedidoEntity;
import com.plantopolis.backend.infrastructure.persistence.entity.PagoEntity;
import com.plantopolis.backend.infrastructure.persistence.entity.PedidoEntity;
import com.plantopolis.backend.infrastructure.persistence.mapper.PedidoMapper;
import com.plantopolis.backend.infrastructure.persistence.repository.DetallePedidoJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.repository.MetodoPagoJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.repository.PagoJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.repository.PedidoJpaRepository;
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

/**
 * Servicio de aplicación que implementa ProcesarPedidoUseCase.
 *
 * CAMBIOS SCHEMA V2:
 *   - Pedido ya no referencia id_carrito (se elimina esa relación).
 *   - Pedido ahora persiste subtotal/impuestos/total como columnas propias
 *     (antes el total se recalculaba en el mapper sumando detalles).
 *   - DetallePedido ahora persiste su propio subtotal.
 *   - El estado "post-checkout" del carrito se renombró de CONVERTIDO (id=2)
 *     a PROCESADO (id=3), por el nuevo orden de inserción en ESTADOCARRITO.
 *   - Todos los métodos de pago quedan APROBADO por simplicidad (se
 *     ajustará cuando se defina la integración real con pasarela de pago).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoService implements ProcesarPedidoUseCase {

    private final UsuarioRepositoryPort  usuarioRepository;
    private final CarritoRepositoryPort  carritoRepository;
    private final ProductoRepositoryPort productoRepository;
    private final PedidoRepositoryPort   pedidoRepository;
    private final NotificacionPort       notificacionPort;

    private final PedidoJpaRepository        pedidoJpaRepo;
    private final DetallePedidoJpaRepository detallePedidoRepo;
    private final PagoJpaRepository          pagoJpaRepo;
    private final MetodoPagoJpaRepository    metodoPagoRepo;
    private final PedidoMapper               pedidoMapper;

    // ── IDs de estados según los INSERT del ────────────────────
    private static final Long ESTADO_PEDIDO_PENDIENTE   = 1L;

    private static final Long ESTADO_CARRITO_PROCESADO  = 3L;

    private static final Long ESTADO_PAGO_APROBADO      = 2L;

    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");

    // ─────────────────────────────────────────────────────────────────────
    /**
     * Enriquece el pago de un pedido con nombre del método y estado.
     * SIMPLIFICADO SCHEMA V2: todos los métodos de pago quedan APROBADO
     * (ya no existe el concepto de "contra entrega" con pago PENDIENTE).
     */
    private void enriquecerPago(Pedido pedido) {
        if (pedido == null || pedido.getIdPedido() == null) return;

        if (pedido.getPago() != null
                && pedido.getPago().getNombreMetodo() != null
                && !pedido.getPago().getNombreMetodo().isEmpty()) {
            return;
        }

        var pagoEntityOpt = pagoJpaRepo.findByIdPedido(pedido.getIdPedido());
        if (pagoEntityOpt.isEmpty()) {
            log.warn("No se encontró pago para pedido {}", pedido.getIdPedido());
            return;
        }

        PagoEntity pagoEntity = pagoEntityOpt.get();
        Long idMetodo = pagoEntity.getIdMetodo();
        if (idMetodo == null) return;

        String nombreMetodo = metodoPagoRepo.findById(idMetodo)
                .map(m -> m.getNombreMetodo())
                .orElse("DESCONOCIDO");

        // SIMPLIFICADO SCHEMA V2: siempre APROBADO por ahora.
        String estadoPago = "APROBADO";

        if (pedido.getPago() != null) {
            pedido.getPago().setNombreMetodo(nombreMetodo);
            pedido.getPago().setEstadoPagoDescripcion(estadoPago);
            pedido.getPago().setIdMetodo(idMetodo);
            if (pedido.getPago().getMonto() == null) {
                pedido.getPago().setMonto(pagoEntity.getMonto());
            }
        } else {
            Pago pago = Pago.builder()
                    .idPago(pagoEntity.getIdPago())
                    .idPedido(pedido.getIdPedido())
                    .idMetodo(idMetodo)
                    .monto(pagoEntity.getMonto())
                    .fechaPago(pagoEntity.getFechaPago())
                    .nombreMetodo(nombreMetodo)
                    .estadoPagoDescripcion(estadoPago)
                    .build();
            pedido.setPago(pago);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public Pedido procesarPedido(String email,
                                  Long idMetodoPago,
                                  String direccionEnvio) {

        // PASO 1: usuario autenticado
        var usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException(
                        "Usuario no encontrado: " + email));

        // PASO 2: carrito activo
        var carrito = carritoRepository
                .buscarCarritoActivo(usuario.getIdUsuario())
                .orElseThrow(() -> new RuntimeException(
                        "No tienes un carrito activo"));

        if (carrito.getItems() == null || carrito.getItems().isEmpty()) {
            throw new RuntimeException("El carrito está vacío");
        }

        // PASO 3: validar método de pago
        var metodoPagoEntity = metodoPagoRepo.findById(idMetodoPago)
                .orElseThrow(() -> new RuntimeException(
                        "Método de pago no válido: " + idMetodoPago));
        String nombreMetodoPago = metodoPagoEntity.getNombreMetodo();

        // PASO 4: validar stock y construir detalles + calcular subtotal/impuestos
        // Cada línea de detalle ahora calcula su propio
        // subtotal (precio*cantidad) y se acumula el subtotal general y
        // los impuestos según el porcentaje_iva de cada producto.
        List<DetallePedidoEntity> detalleEntities = new ArrayList<>();
        BigDecimal subtotalPedido = BigDecimal.ZERO;
        BigDecimal impuestosPedido = BigDecimal.ZERO;

        for (var item : carrito.getItems()) {
            var producto = productoRepository
                    .buscarPorId(item.getIdProducto())
                    .orElseThrow(() -> new RuntimeException(
                            "Producto no encontrado: " + item.getIdProducto()));

            if (!Boolean.TRUE.equals(producto.getActivo())) {
                throw new RuntimeException(
                        "Producto no disponible: " + producto.getNombreProducto());
            }
            if (producto.getStock() < item.getCantidad()) {
                throw new RuntimeException(
                        "Stock insuficiente para '" + producto.getNombreProducto()
                        + "'. Disponible: " + producto.getStock());
            }

            BigDecimal subtotalLinea = item.getPrecioUnitario()
                    .multiply(BigDecimal.valueOf(item.getCantidad()));

            // Porcentaje de IVA del producto (por defecto 19% si viene null)
            BigDecimal porcentajeIva = producto.getPorcentajeIva() != null
                    ? producto.getPorcentajeIva()
                    : new BigDecimal("19.00");

            BigDecimal impuestoLinea = subtotalLinea
                    .multiply(porcentajeIva)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            subtotalPedido = subtotalPedido.add(subtotalLinea);
            impuestosPedido = impuestosPedido.add(impuestoLinea);

            detalleEntities.add(DetallePedidoEntity.builder()
                    .idProducto(item.getIdProducto())
                    .cantidad(item.getCantidad())
                    .precioUnitario(item.getPrecioUnitario())
                    // NUEVO SCHEMA V2: subtotal persistido por línea
                    .subtotal(subtotalLinea)
                    .build());
        }

        BigDecimal totalPedido = subtotalPedido.add(impuestosPedido);

        // PASO 5: número de pedido y fecha
        String numeroPedido = pedidoRepository.generarNumeroPedido();
        LocalDateTime fechaPedido = LocalDateTime.now(ZONA_BOGOTA);

        // PASO 6: insertar el Pedido
        var savedPedido = pedidoJpaRepo.saveAndFlush(
                PedidoEntity.builder()
                        .idUsuario(usuario.getIdUsuario())
                        .idEstado(ESTADO_PEDIDO_PENDIENTE)
                        .fechaPedido(fechaPedido)
                        .direccionEnvio(direccionEnvio)
                        .numeroPedido(numeroPedido)
                        // NUEVO SCHEMA V2: columnas NOT NULL calculadas arriba
                        .subtotal(subtotalPedido)
                        .impuestos(impuestosPedido)
                        .total(totalPedido)
                        .build());

        Long idPedidoGenerado = savedPedido.getIdPedido();

        // PASO 7: guardar detalles con la FK del pedido
        detalleEntities.forEach(d -> d.setIdPedido(idPedidoGenerado));
        detallePedidoRepo.saveAll(detalleEntities);

        // PASO 8: registrar el pago
        // SIMPLIFICADO SCHEMA V2: siempre queda APROBADO (id=2).
        var pagoEntity = PagoEntity.builder()
                .idPedido(idPedidoGenerado)
                .idMetodo(idMetodoPago)
                .idEstadoPago(ESTADO_PAGO_APROBADO)
                .monto(totalPedido)
                .fechaPago(LocalDateTime.now(ZONA_BOGOTA))
                .build();
        pagoJpaRepo.save(pagoEntity);

        // PASO 9: reducir stock
        carrito.getItems().forEach(item ->
            productoRepository.buscarPorId(item.getIdProducto())
                    .ifPresent(p -> {
                        p.setStock(p.getStock() - item.getCantidad());
                        productoRepository.guardar(p);
                    })
        );

        // PASO 10: marcar carrito como PROCESADO (antes CONVERTIDO)
        carritoRepository.cambiarEstado(
                carrito.getIdCarrito(), ESTADO_CARRITO_PROCESADO);
        log.debug("Carrito {} → PROCESADO", carrito.getIdCarrito());

        // PASO 11: recargar pedido completo
        var pedidoRespuesta = pedidoRepository
                .buscarPorId(idPedidoGenerado)
                .orElseThrow(() -> new RuntimeException(
                        "Error recuperando pedido: " + idPedidoGenerado));

        enriquecerPago(pedidoRespuesta);

        if (pedidoRespuesta.getPago() != null) {
            pedidoRespuesta.getPago().setNombreMetodo(nombreMetodoPago);
            pedidoRespuesta.getPago().setEstadoPagoDescripcion("APROBADO");
        }

        if (pedidoRespuesta.getEmailCliente() == null) {
            pedidoRespuesta.setEmailCliente(usuario.getCorreo());
            pedidoRespuesta.setNombreCliente(usuario.getNombreCompleto());
        }

        // PASO 12: construir objeto para el email de confirmación
        List<DetallePedido> detallesEmail = carrito.getItems().stream()
                .map(item -> DetallePedido.builder()
                        .idProducto(item.getIdProducto())
                        .nombreProducto(item.getNombreProducto())
                        .imagenUrl(item.getImagenUrl())
                        .cantidad(item.getCantidad())
                        .precioUnitario(item.getPrecioUnitario())
                        .subtotal(item.getPrecioUnitario()
                                .multiply(BigDecimal.valueOf(item.getCantidad())))
                        .build())
                .toList();

        Pedido pedidoEmail = Pedido.builder()
                .idPedido(idPedidoGenerado)
                .numeroPedido(numeroPedido)
                .estadoDescripcion("PENDIENTE")
                .fechaPedido(fechaPedido)
                .direccionEnvio(direccionEnvio)
                .emailCliente(usuario.getCorreo())
                .nombreCliente(usuario.getNombreCompleto())
                .detalles(detallesEmail)
                .subtotal(subtotalPedido)
                .impuestos(impuestosPedido)
                .pago(Pago.builder()
                        .idPedido(idPedidoGenerado)
                        .idMetodo(idMetodoPago)
                        .monto(totalPedido)
                        .fechaPago(pagoEntity.getFechaPago())
                        .nombreMetodo(nombreMetodoPago)
                        .estadoPagoDescripcion("APROBADO")
                        .build())
                .total(totalPedido)
                .build();

        // PASO 13: enviar email (falla silenciosamente)
        try {
            notificacionPort.enviarConfirmacionPedido(pedidoEmail);
            log.info("✅ Email enviado a: {}", usuario.getCorreo());
        } catch (Exception e) {
            log.warn("⚠ Error enviando email (pedido: {}): {}",
                    numeroPedido, e.getMessage());
        }

        return pedidoRespuesta;
    }

    // ─────────────────────────────────────────────────────────────────────
    @Override
    public List<Pedido> obtenerHistorial(String email) {
        var usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException(
                        "Usuario no encontrado: " + email));

        List<Pedido> pedidos = pedidoRepository
                .buscarPorUsuario(usuario.getIdUsuario());

        pedidos.forEach(this::enriquecerPago);

        return pedidos;
    }

    // ─────────────────────────────────────────────────────────────────────
    @Override
    public Pedido obtenerDetalle(String email, Long idPedido) {
        var usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException(
                        "Usuario no encontrado: " + email));

        var pedido = pedidoRepository.buscarPorId(idPedido)
                .orElseThrow(() -> new RuntimeException(
                        "Pedido no encontrado: " + idPedido));

        if (!pedido.getIdUsuario().equals(usuario.getIdUsuario())) {
            throw new RuntimeException(
                    "No tienes permisos para ver este pedido");
        }

        enriquecerPago(pedido);

        return pedido;
    }
}