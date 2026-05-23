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
import java.time.LocalDateTime;
import java.time.ZoneId; // ← importar ZoneId para manejar zonas horarias
import java.util.List;

/**
 * Servicio de aplicación que implementa ProcesarPedidoUseCase.
 *
 * TIMEZONE FIX:
 *   Todas las fechas (fechaPedido y fechaPago) se generan con
 *   LocalDateTime.now(ZONA_BOGOTA) para que Oracle almacene la hora
 *   de Colombia (UTC-5) y no la hora UTC del servidor Docker.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoService implements ProcesarPedidoUseCase {

    // ── Puertos del dominio ───────────────────────────────────────────────────
    private final UsuarioRepositoryPort  usuarioRepository;
    private final CarritoRepositoryPort  carritoRepository;
    private final ProductoRepositoryPort productoRepository;
    private final PedidoRepositoryPort   pedidoRepository;
    private final NotificacionPort       notificacionPort;

    // ── Repositorios JPA directos ─────────────────────────────────────────────
    private final PedidoJpaRepository        pedidoJpaRepo;
    private final DetallePedidoJpaRepository detallePedidoRepo;
    private final PagoJpaRepository          pagoJpaRepo;
    private final MetodoPagoJpaRepository    metodoPagoRepo;
    private final PedidoMapper               pedidoMapper;

    // ── IDs de estados según datos iniciales del SQL ──────────────────────────
    private static final Long ESTADO_PEDIDO_PENDIENTE   = 1L;
    private static final Long ESTADO_CARRITO_CONVERTIDO = 2L;
    private static final Long ESTADO_PAGO_APROBADO      = 2L;
    private static final Long ID_METODO_EFECTIVO        = 4L;

    // ── Zona horaria de Colombia ──────────────────────────────────────────────
    // Colombia no usa horario de verano, siempre UTC-5
    // Se aplica a TODAS las fechas que se persisten en Oracle
    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");

    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Enriquece el pago de un pedido con nombre del método y estado.
     * Carga el PagoEntity directamente desde BD usando el ID del pedido,
     * sin depender de relaciones JPA que pueden quedar null por caché de Hibernate.
     */
    private void enriquecerPago(Pedido pedido) {
        if (pedido == null || pedido.getIdPedido() == null) return;

        // Si el pago ya tiene el nombre del método, no hacer nada
        if (pedido.getPago() != null
                && pedido.getPago().getNombreMetodo() != null
                && !pedido.getPago().getNombreMetodo().isEmpty()) {
            log.debug("Pago ya enriquecido: {}", pedido.getPago().getNombreMetodo());
            return;
        }

        // Cargar el PagoEntity directamente por idPedido (query simple por columna)
        var pagoEntityOpt = pagoJpaRepo.findByIdPedido(pedido.getIdPedido());

        if (pagoEntityOpt.isEmpty()) {
            log.warn("No se encontró pago para pedido {}", pedido.getIdPedido());
            return;
        }

        PagoEntity pagoEntity = pagoEntityOpt.get();
        Long idMetodo = pagoEntity.getIdMetodo();

        if (idMetodo == null) {
            log.warn("PagoEntity sin idMetodo para pedido {}", pedido.getIdPedido());
            return;
        }

        // Cargar el nombre del método directamente desde MetodoPagoJpaRepository
        String nombreMetodo = metodoPagoRepo.findById(idMetodo)
                .map(m -> m.getNombreMetodo())
                .orElse("DESCONOCIDO");

        // EFECTIVO (id=4) = Contra entrega = pago pendiente hasta recibir físicamente
        // Todos los demás = pago electrónico = APROBADO inmediatamente
        String estadoPago = ID_METODO_EFECTIVO.equals(idMetodo)
                ? "PENDIENTE" : "APROBADO";

        log.debug("Pago enriquecido: método={} (id={}), estado={}",
                nombreMetodo, idMetodo, estadoPago);

        // Actualizar el dominio Pago con los datos cargados desde BD
        if (pedido.getPago() != null) {
            pedido.getPago().setNombreMetodo(nombreMetodo);
            pedido.getPago().setEstadoPagoDescripcion(estadoPago);
            pedido.getPago().setIdMetodo(idMetodo);
            if (pedido.getPago().getMonto() == null) {
                pedido.getPago().setMonto(pagoEntity.getMonto());
            }
        } else {
            // Construir el objeto Pago completo si no venía cargado
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

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public Pedido procesarPedido(String email,
                                  Long idMetodoPago,
                                  String direccionEnvio) {

        // PASO 1: Obtener usuario autenticado por email (del token JWT)
        var usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException(
                        "Usuario no encontrado: " + email));

        // PASO 2: Obtener el carrito activo del usuario
        var carrito = carritoRepository
                .buscarCarritoActivo(usuario.getIdUsuario())
                .orElseThrow(() -> new RuntimeException(
                        "No tienes un carrito activo"));

        // Validar que el carrito tenga al menos un ítem
        if (carrito.getItems() == null || carrito.getItems().isEmpty()) {
            throw new RuntimeException("El carrito está vacío");
        }

        // PASO 3: Validar método de pago y obtener su nombre
        var metodoPagoEntity = metodoPagoRepo.findById(idMetodoPago)
                .orElseThrow(() -> new RuntimeException(
                        "Método de pago no válido: " + idMetodoPago));
        String nombreMetodoPago = metodoPagoEntity.getNombreMetodo();
        log.debug("Checkout con método: {} (id={})", nombreMetodoPago, idMetodoPago);

        // PASO 4: Validar stock de cada producto y construir la lista de detalles
        List<DetallePedidoEntity> detalleEntities = carrito.getItems()
                .stream()
                .map(item -> {
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

                    // Crear el detalle con precio al momento de la compra (snapshot)
                    return DetallePedidoEntity.builder()
                            .idProducto(item.getIdProducto())
                            .cantidad(item.getCantidad())
                            .precioUnitario(item.getPrecioUnitario())
                            .build();
                })
                .toList();

        // PASO 5: Calcular el total del pedido desde el carrito
        BigDecimal total = carrito.getTotal();

        // PASO 6: Generar número de pedido único y capturar la fecha en hora Bogotá
        String numeroPedido = pedidoRepository.generarNumeroPedido();

        // ── FIX TIMEZONE ──────────────────────────────────────────────────────
        // LocalDateTime.now() sin zona usa UTC en Docker → hora incorrecta.
        // LocalDateTime.now(ZONA_BOGOTA) garantiza hora colombiana (UTC-5).
        // Aplica tanto a la fecha del pedido como a la fecha del pago.
        LocalDateTime fechaPedido = LocalDateTime.now(ZONA_BOGOTA);

        // PASO 7: Insertar el Pedido en Oracle y obtener el ID generado
        var savedPedido = pedidoJpaRepo.saveAndFlush(
                PedidoEntity.builder()
                        .idUsuario(usuario.getIdUsuario())
                        .idEstado(ESTADO_PEDIDO_PENDIENTE) // estado inicial: PENDIENTE
                        .idCarrito(carrito.getIdCarrito())
                        .fechaPedido(fechaPedido)           // ← hora Colombia
                        .direccionEnvio(direccionEnvio)
                        .numeroPedido(numeroPedido)
                        .build());

        Long idPedidoGenerado = savedPedido.getIdPedido();
        log.debug("Pedido insertado con ID: {}", idPedidoGenerado);

        // PASO 8: Guardar los detalles con la FK del pedido recién creado
        detalleEntities.forEach(d -> d.setIdPedido(idPedidoGenerado));
        detallePedidoRepo.saveAll(detalleEntities);

        // PASO 9: Registrar el pago con fecha en hora de Bogotá
        var pagoEntity = PagoEntity.builder()
                .idPedido(idPedidoGenerado)
                .idMetodo(idMetodoPago)
                .idEstadoPago(ESTADO_PAGO_APROBADO)
                .monto(total)
                // ── FIX TIMEZONE ──────────────────────────────────────────
                // Igual que fechaPedido: usar ZONA_BOGOTA para hora Colombia
                .fechaPago(LocalDateTime.now(ZONA_BOGOTA))
                .build();
        pagoJpaRepo.save(pagoEntity);

        // PASO 10: Reducir el stock de cada producto comprado
        carrito.getItems().forEach(item ->
            productoRepository.buscarPorId(item.getIdProducto())
                    .ifPresent(p -> {
                        p.setStock(p.getStock() - item.getCantidad());
                        productoRepository.guardar(p);
                    })
        );

        // PASO 11: Marcar el carrito como CONVERTIDO para que no reaparezca
        carritoRepository.cambiarEstado(
                carrito.getIdCarrito(), ESTADO_CARRITO_CONVERTIDO);
        log.debug("Carrito {} → CONVERTIDO", carrito.getIdCarrito());

        // PASO 12: Recargar el pedido completo desde BD con todas sus relaciones
        var pedidoRespuesta = pedidoRepository
                .buscarPorId(idPedidoGenerado)
                .orElseThrow(() -> new RuntimeException(
                        "Error recuperando pedido: " + idPedidoGenerado));

        // PASO 13: Enriquecer el pago con nombre del método y estado
        enriquecerPago(pedidoRespuesta);

        // Forzar datos del pago desde memoria (más confiable dentro de la misma transacción)
        if (pedidoRespuesta.getPago() != null) {
            pedidoRespuesta.getPago().setNombreMetodo(nombreMetodoPago);
            pedidoRespuesta.getPago().setEstadoPagoDescripcion(
                    ID_METODO_EFECTIVO.equals(idMetodoPago) ? "PENDIENTE" : "APROBADO");
        }

        if (pedidoRespuesta.getEmailCliente() == null) {
            pedidoRespuesta.setEmailCliente(usuario.getCorreo());
            pedidoRespuesta.setNombreCliente(usuario.getNombreCompleto());
        }

        // PASO 14: Construir el objeto Pedido para el email con datos en memoria
        String estadoPagoEmail = ID_METODO_EFECTIVO.equals(idMetodoPago)
                ? "PENDIENTE" : "APROBADO";

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
                .fechaPedido(fechaPedido)          // ya es hora Colombia
                .direccionEnvio(direccionEnvio)
                .emailCliente(usuario.getCorreo())
                .nombreCliente(usuario.getNombreCompleto())
                .detalles(detallesEmail)
                .pago(Pago.builder()
                        .idPedido(idPedidoGenerado)
                        .idMetodo(idMetodoPago)
                        .monto(total)
                        .fechaPago(pagoEntity.getFechaPago()) // ya es hora Colombia
                        .nombreMetodo(nombreMetodoPago)
                        .estadoPagoDescripcion(estadoPagoEmail)
                        .build())
                .total(total)
                .build();

        // PASO 15: Enviar email de confirmación (falla silenciosamente)
        try {
            notificacionPort.enviarConfirmacionPedido(pedidoEmail);
            log.info("✅ Email enviado a: {}", usuario.getCorreo());
        } catch (Exception e) {
            log.warn("⚠ Error enviando email (pedido: {}): {}",
                    numeroPedido, e.getMessage());
        }

        return pedidoRespuesta;
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public List<Pedido> obtenerHistorial(String email) {
        var usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException(
                        "Usuario no encontrado: " + email));

        List<Pedido> pedidos = pedidoRepository
                .buscarPorUsuario(usuario.getIdUsuario());

        // Enriquecer el método de pago de cada pedido en el historial
        pedidos.forEach(this::enriquecerPago);

        return pedidos;
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public Pedido obtenerDetalle(String email, Long idPedido) {
        var usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException(
                        "Usuario no encontrado: " + email));

        var pedido = pedidoRepository.buscarPorId(idPedido)
                .orElseThrow(() -> new RuntimeException(
                        "Pedido no encontrado: " + idPedido));

        // Verificar que el pedido pertenezca al usuario autenticado
        if (!pedido.getIdUsuario().equals(usuario.getIdUsuario())) {
            throw new RuntimeException(
                    "No tienes permisos para ver este pedido");
        }

        // Enriquecer el método de pago cargando directamente desde BD
        enriquecerPago(pedido);

        return pedido;
    }
}