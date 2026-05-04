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
import java.util.List;

/**
 * Servicio de aplicación que implementa ProcesarPedidoUseCase.
 *
 * SOLUCIÓN DEFINITIVA AL MÉTODO DE PAGO:
 *   En lugar de depender de las relaciones JPA (que pueden quedar null
 *   por el caché de Hibernate), enriquecerPago() carga el pago y su
 *   método DIRECTAMENTE desde PagoJpaRepository y MetodoPagoJpaRepository
 *   usando los IDs de las columnas (no las relaciones @ManyToOne).
 *   Esto es 100% confiable porque usa los valores de columna simples.
 *
 * Ruta destino:
 *   Back/src/main/java/com/plantopolis/backend/application/service/PedidoService.java
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
    private final PagoJpaRepository          pagoJpaRepo;       // para cargar pago por idPedido
    private final MetodoPagoJpaRepository    metodoPagoRepo;    // para cargar nombre del método
    private final PedidoMapper               pedidoMapper;

    // ── IDs de estados según datos iniciales del SQL ──────────────────────────
    private static final Long ESTADO_PEDIDO_PENDIENTE   = 1L;
    private static final Long ESTADO_CARRITO_CONVERTIDO = 2L;
    private static final Long ESTADO_PAGO_APROBADO      = 2L;
    private static final Long ID_METODO_EFECTIVO        = 4L; // Contra entrega

    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Enriquece el pago de un pedido con nombre del método y estado.
     *
     * ESTRATEGIA (de más confiable a menos):
     *   1. Cargar el PagoEntity directamente desde BD usando idPedido
     *      (no depende de relaciones JPA, solo del valor de columna ID_PEDIDO)
     *   2. Con el ID_METODO del PagoEntity, cargar MetodoPagoEntity directamente
     *   3. Asignar nombreMetodo y estadoPago al dominio Pago
     *
     * Este método se aplica después de cargar cualquier pedido desde BD.
     * No usa relaciones @ManyToOne ni JOIN FETCH, solo queries simples por ID.
     *
     * @param pedido pedido del dominio al que enriquecer el pago
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
        // Esto es 100% confiable porque usa findByIdPedido(), no una relación JPA
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

        // Determinar estado del pago:
        // EFECTIVO (id=4) = Contra entrega = PENDIENTE hasta recibir físicamente
        // Todos los demás = pago electrónico = APROBADO inmediatamente
        String estadoPago = ID_METODO_EFECTIVO.equals(idMetodo)
                ? "PENDIENTE" : "APROBADO";

        log.debug("Pago enriquecido: método={} (id={}), estado={}",
                nombreMetodo, idMetodo, estadoPago);

        // Si el dominio ya tiene un objeto Pago, actualizarlo
        if (pedido.getPago() != null) {
            pedido.getPago().setNombreMetodo(nombreMetodo);
            pedido.getPago().setEstadoPagoDescripcion(estadoPago);
            pedido.getPago().setIdMetodo(idMetodo);
            if (pedido.getPago().getMonto() == null) {
                pedido.getPago().setMonto(pagoEntity.getMonto());
            }
        } else {
            // Si el pago no se cargó desde BD, construirlo completo desde la entidad
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

        // PASO 1: Obtener usuario
        var usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException(
                        "Usuario no encontrado: " + email));

        // PASO 2: Obtener carrito activo
        var carrito = carritoRepository
                .buscarCarritoActivo(usuario.getIdUsuario())
                .orElseThrow(() -> new RuntimeException(
                        "No tienes un carrito activo"));

        if (carrito.getItems() == null || carrito.getItems().isEmpty()) {
            throw new RuntimeException("El carrito está vacío");
        }

        // PASO 3: Validar método de pago y obtener nombre en memoria
        var metodoPagoEntity = metodoPagoRepo.findById(idMetodoPago)
                .orElseThrow(() -> new RuntimeException(
                        "Método de pago no válido: " + idMetodoPago));
        String nombreMetodoPago = metodoPagoEntity.getNombreMetodo();
        log.debug("Checkout con método: {} (id={})", nombreMetodoPago, idMetodoPago);

        // PASO 4: Validar stock y construir detalles
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

                    return DetallePedidoEntity.builder()
                            .idProducto(item.getIdProducto())
                            .cantidad(item.getCantidad())
                            .precioUnitario(item.getPrecioUnitario())
                            .build();
                })
                .toList();

        // PASO 5: Calcular total
        BigDecimal total = carrito.getTotal();

        // PASO 6: Generar número de pedido y fecha
        String numeroPedido = pedidoRepository.generarNumeroPedido();
        LocalDateTime fechaPedido = LocalDateTime.now();

        // PASO 7: INSERT Pedido → obtener ID de Oracle
        var savedPedido = pedidoJpaRepo.saveAndFlush(
                PedidoEntity.builder()
                        .idUsuario(usuario.getIdUsuario())
                        .idEstado(ESTADO_PEDIDO_PENDIENTE)
                        .idCarrito(carrito.getIdCarrito())
                        .fechaPedido(fechaPedido)
                        .direccionEnvio(direccionEnvio)
                        .numeroPedido(numeroPedido)
                        .build());

        Long idPedidoGenerado = savedPedido.getIdPedido();
        log.debug("Pedido insertado con ID: {}", idPedidoGenerado);

        // PASO 8: Guardar detalles con FK del pedido
        detalleEntities.forEach(d -> d.setIdPedido(idPedidoGenerado));
        detallePedidoRepo.saveAll(detalleEntities);

        // PASO 9: Guardar pago
        var pagoEntity = PagoEntity.builder()
                .idPedido(idPedidoGenerado)
                .idMetodo(idMetodoPago)
                .idEstadoPago(ESTADO_PAGO_APROBADO)
                .monto(total)
                .fechaPago(LocalDateTime.now())
                .build();
        pagoJpaRepo.save(pagoEntity);

        // PASO 10: Reducir stock
        carrito.getItems().forEach(item ->
            productoRepository.buscarPorId(item.getIdProducto())
                    .ifPresent(p -> {
                        p.setStock(p.getStock() - item.getCantidad());
                        productoRepository.guardar(p);
                    })
        );

        // PASO 11: Marcar carrito como CONVERTIDO
        carritoRepository.cambiarEstado(
                carrito.getIdCarrito(), ESTADO_CARRITO_CONVERTIDO);
        log.debug("Carrito {} → CONVERTIDO", carrito.getIdCarrito());

        // PASO 12: Cargar pedido desde BD
        var pedidoRespuesta = pedidoRepository
                .buscarPorId(idPedidoGenerado)
                .orElseThrow(() -> new RuntimeException(
                        "Error recuperando pedido: " + idPedidoGenerado));

        // PASO 13: Enriquecer pago de la respuesta
        // Primero intentar con enriquecerPago(), luego forzar desde memoria
        enriquecerPago(pedidoRespuesta);

        // Forzar desde memoria (más confiable dentro de la misma transacción)
        if (pedidoRespuesta.getPago() != null) {
            pedidoRespuesta.getPago().setNombreMetodo(nombreMetodoPago);
            pedidoRespuesta.getPago().setEstadoPagoDescripcion(
                    ID_METODO_EFECTIVO.equals(idMetodoPago) ? "PENDIENTE" : "APROBADO");
        }

        if (pedidoRespuesta.getEmailCliente() == null) {
            pedidoRespuesta.setEmailCliente(usuario.getCorreo());
            pedidoRespuesta.setNombreCliente(usuario.getNombreCompleto());
        }

        // PASO 14: Construir pedido para el EMAIL con datos en memoria
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
                .fechaPedido(fechaPedido)
                .direccionEnvio(direccionEnvio)
                .emailCliente(usuario.getCorreo())
                .nombreCliente(usuario.getNombreCompleto())
                .detalles(detallesEmail)
                .pago(Pago.builder()
                        .idPedido(idPedidoGenerado)
                        .idMetodo(idMetodoPago)
                        .monto(total)
                        .fechaPago(pagoEntity.getFechaPago())
                        .nombreMetodo(nombreMetodoPago)
                        .estadoPagoDescripcion(estadoPagoEmail)
                        .build())
                .total(total)
                .build();

        // PASO 15: Enviar email (silencioso si falla)
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

        // Enriquecer el método de pago de cada pedido
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

        if (!pedido.getIdUsuario().equals(usuario.getIdUsuario())) {
            throw new RuntimeException(
                    "No tienes permisos para ver este pedido");
        }

        // Enriquecer el método de pago cargando directamente desde BD
        enriquecerPago(pedido);

        return pedido;
    }
}