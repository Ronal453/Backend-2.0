package com.plantopolis.backend.application.service;

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
 * Orquesta el flujo completo de checkout:
 *   1. Valida usuario, carrito, método de pago y stock
 *   2. Persiste Pedido → DetallePedido → Pago (en ese orden, con IDs explícitos)
 *   3. Reduce stock, marca carrito como CONVERTIDO
 *   4. Envía email de confirmación (silencioso si falla)
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoService implements ProcesarPedidoUseCase {

    // ── Puertos del dominio (interfaces hexagonales) ──────────────────────────
    private final UsuarioRepositoryPort     usuarioRepository;    // acceso a usuarios
    private final CarritoRepositoryPort     carritoRepository;    // acceso al carrito
    private final ProductoRepositoryPort    productoRepository;   // acceso a productos
    private final PedidoRepositoryPort      pedidoRepository;     // acceso a pedidos (dominio)
    private final NotificacionPort          notificacionPort;     // envío de emails

    // ── Repositorios JPA directos (para persistir con control total de FKs) ──
    private final PedidoJpaRepository           pedidoJpaRepo;        // guardar el pedido
    private final DetallePedidoJpaRepository    detallePedidoRepo;    // guardar detalles directamente
    private final PagoJpaRepository             pagoJpaRepo;          // guardar pago directamente
    private final MetodoPagoJpaRepository       metodoPagoRepo;       // validar método de pago
    private final PedidoMapper                  pedidoMapper;         // conversión entity → dominio

    // ── Constantes de IDs según datos iniciales del SQL (INSERT INTO ...) ────
    /** ID del estado PENDIENTE en tabla ESTADOPEDIDO */
    private static final Long ESTADO_PEDIDO_PENDIENTE   = 1L;

    /** ID del estado CONVERTIDO en tabla ESTADOCARRITO (carrito procesado en checkout) */
    private static final Long ESTADO_CARRITO_CONVERTIDO = 2L;

    /** ID del estado APROBADO en tabla ESTADOPAGO */
    private static final Long ESTADO_PAGO_APROBADO      = 2L;

    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Procesa el checkout: convierte el carrito activo en un Pedido confirmado.
     *
     * FLUJO CORREGIDO (evita ORA-01400):
     *   Paso 1-6  → validaciones
     *   Paso 7    → INSERT Pedido (sin detalles) → obtiene idPedido de Oracle
     *   Paso 8    → asigna idPedido a cada DetallePedidoEntity → INSERT detalles
     *   Paso 9    → INSERT Pago con idPedido ya conocido
     *   Paso 10   → reduce stock
     *   Paso 11   → marca carrito CONVERTIDO
     *   Paso 12   → carga pedido completo con relaciones para la respuesta
     *   Paso 13   → email silencioso
     *
     * @param email          correo del usuario autenticado (viene del JWT)
     * @param idMetodoPago   1=TARJETA_CREDITO, 2=TARJETA_DEBITO, 3=TRANSFERENCIA, 4=EFECTIVO
     * @param direccionEnvio dirección de entrega del pedido
     * @return dominio Pedido con todos los detalles y pago incluidos
     */
    @Override
    @Transactional  // todo el proceso es atómico; si falla algo, se hace rollback completo
    public Pedido procesarPedido(String email,
                                  Long idMetodoPago,
                                  String direccionEnvio) {

        // ── PASO 1: Obtener usuario autenticado ───────────────────────────────
        // El email viene del token JWT extraído en el controller (@AuthenticationPrincipal)
        var usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException(
                        "Usuario no encontrado: " + email));

        // ── PASO 2: Obtener carrito activo con sus ítems ──────────────────────
        // Solo el carrito en estado ACTIVO (idEstadoCarrito = 1) puede usarse
        var carrito = carritoRepository
                .buscarCarritoActivo(usuario.getIdUsuario())
                .orElseThrow(() -> new RuntimeException(
                        "No tienes un carrito activo"));

        // Validar que el carrito no esté vacío antes de proceder
        if (carrito.getItems() == null || carrito.getItems().isEmpty()) {
            throw new RuntimeException("El carrito está vacío");
        }

        // ── PASO 3: Validar que el método de pago existe en BD ────────────────
        metodoPagoRepo.findById(idMetodoPago)
                .orElseThrow(() -> new RuntimeException(
                        "Método de pago no válido: " + idMetodoPago));

        // ── PASO 4: Validar stock y construir entidades de detalle ────────────
        // IMPORTANTE: NO se asigna idPedido aquí porque aún no tenemos el ID
        // generado por Oracle. Se asignará en el paso 8 después del saveAndFlush.
        List<DetallePedidoEntity> detalleEntities = carrito.getItems()
                .stream()
                .map(item -> {
                    // Verificar que el producto existe y está activo
                    var producto = productoRepository
                            .buscarPorId(item.getIdProducto())
                            .orElseThrow(() -> new RuntimeException(
                                    "Producto no encontrado: " + item.getIdProducto()));

                    // Rechazar productos desactivados por el administrador
                    if (!Boolean.TRUE.equals(producto.getActivo())) {
                        throw new RuntimeException(
                                "Producto no disponible: " + producto.getNombreProducto());
                    }

                    // Validar que haya suficiente stock antes de confirmar el pedido
                    if (producto.getStock() < item.getCantidad()) {
                        throw new RuntimeException(
                                "Stock insuficiente para '"
                                + producto.getNombreProducto()
                                + "'. Disponible: " + producto.getStock());
                    }

                    // Construir el detalle con el precio actual del carrito
                    // (precio snapshot: no cambia aunque el precio del producto cambie después)
                    return DetallePedidoEntity.builder()
                            .idProducto(item.getIdProducto())        // FK → PRODUCTO
                            .cantidad(item.getCantidad())             // unidades compradas
                            .precioUnitario(item.getPrecioUnitario()) // precio al momento de compra
                            // idPedido se asigna en paso 8, después de obtener el ID generado
                            .build();
                })
                .toList();

        // ── PASO 5: Calcular el total del pedido ──────────────────────────────
        // El carrito ya tiene el total pre-calculado (suma de subtotales de cada ítem)
        BigDecimal total = carrito.getTotal();

        // ── PASO 6: Generar número de pedido único ────────────────────────────
        // Formato: PL-YYYYMMDD-XXXX (ej: PL-20241215-4823)
        // El repositorio garantiza unicidad reintentando si ya existe
        String numeroPedido = pedidoRepository.generarNumeroPedido();

        // ── PASO 7: Persistir el Pedido SIN detalles para obtener el ID ───────
        //
        // POR QUÉ esto en dos pasos:
        //   Oracle genera el ID con IDENTITY (autoincrement). Hibernate necesita
        //   hacer el INSERT del Pedido primero para obtener ese ID. Solo entonces
        //   podemos asignarlo como FK a los DetallePedido.
        //
        // POR QUÉ NO cascade:
        //   @OneToMany @JoinColumn unidireccional en PedidoEntity hace:
        //     INSERT DETALLEPEDIDO (...) sin ID_PEDIDO → ORA-01400 (NOT NULL violation)
        //     UPDATE DETALLEPEDIDO SET ID_PEDIDO = ?   → demasiado tarde
        //
        var pedidoEntity = PedidoEntity.builder()
                .idUsuario(usuario.getIdUsuario())       // FK → USUARIO
                .idEstado(ESTADO_PEDIDO_PENDIENTE)        // Estado inicial: PENDIENTE (id=1)
                .idCarrito(carrito.getIdCarrito())        // FK → CARRITO (para auditoría)
                .fechaPedido(LocalDateTime.now())         // Timestamp del momento del pedido
                .direccionEnvio(direccionEnvio)           // Dirección que envió el cliente
                .numeroPedido(numeroPedido)               // Código legible: PL-20241215-4823
                .build();

        // saveAndFlush: INSERT inmediato + flush del contexto de Hibernate
        // Esto garantiza que Oracle asigna el ID antes de continuar
        var savedPedido = pedidoJpaRepo.saveAndFlush(pedidoEntity);

        // Capturar el ID autogenerado por Oracle IDENTITY
        Long idPedidoGenerado = savedPedido.getIdPedido();
        log.debug("Pedido creado con ID: {} y número: {}", idPedidoGenerado, numeroPedido);

        // ── PASO 8: Asignar FK y persistir DetallePedido directamente ─────────
        //
        // Ahora que tenemos idPedidoGenerado, lo asignamos a cada detalle
        // y los insertamos directamente con detallePedidoRepo.saveAll().
        //
        // Esto genera: INSERT DETALLEPEDIDO (id_pedido, id_producto, cantidad, precio_unitario)
        //   VALUES (?, ?, ?, ?)   ← todos los campos completos, sin null → SIN ORA-01400
        detalleEntities.forEach(d -> d.setIdPedido(idPedidoGenerado));
        detallePedidoRepo.saveAll(detalleEntities);
        log.debug("Guardados {} detalles para el pedido {}", detalleEntities.size(), idPedidoGenerado);

        // ── PASO 9: Construir y persistir el Pago directamente ────────────────
        //
        // Similar al caso de los detalles: el Pago tiene FK (ID_PEDIDO) que debe
        // conocerse antes del INSERT. Se guarda con pagoJpaRepo directamente.
        //
        // La relación @OneToOne en PedidoEntity tiene insertable=false/updatable=false
        // por eso no puede usarse el cascade para insertar el pago.
        var pagoEntity = PagoEntity.builder()
                .idPedido(idPedidoGenerado)          // FK → PEDIDO (ya conocido)
                .idMetodo(idMetodoPago)               // FK → METODOPAGO (validado en paso 3)
                .idEstadoPago(ESTADO_PAGO_APROBADO)   // Estado: APROBADO (id=2) — pago simulado
                .monto(total)                         // Monto total del pedido
                .fechaPago(LocalDateTime.now())        // Timestamp del pago
                .build();

        pagoJpaRepo.save(pagoEntity);
        log.debug("Pago registrado para el pedido {}, monto: {}", idPedidoGenerado, total);

        // ── PASO 10: Reducir stock de cada producto ────────────────────────────
        // Se decrementa el stock DESPUÉS de confirmar que el pedido se persistió.
        // Si hubiera una excepción aquí, @Transactional haría rollback de todo.
        carrito.getItems().forEach(item ->
            productoRepository.buscarPorId(item.getIdProducto())
                    .ifPresent(p -> {
                        // Restar la cantidad comprada del stock disponible
                        p.setStock(p.getStock() - item.getCantidad());
                        productoRepository.guardar(p);
                        log.debug("Stock reducido: producto {} → nuevo stock: {}",
                                p.getIdProducto(), p.getStock());
                    })
        );

        // ── PASO 11: Marcar carrito como CONVERTIDO ───────────────────────────
        // El carrito pasa de ACTIVO (1) → CONVERTIDO (2).
        // Esto evita que el mismo carrito pueda usarse en otro checkout.
        carritoRepository.cambiarEstado(carrito.getIdCarrito(), ESTADO_CARRITO_CONVERTIDO);
        log.debug("Carrito {} marcado como CONVERTIDO", carrito.getIdCarrito());

        // ── PASO 12: Recargar el pedido completo desde BD ─────────────────────
        // Se hace una nueva consulta para obtener el pedido con TODAS sus
        // relaciones cargadas (detalles + pago + estado + usuario),
        // necesario para construir la respuesta completa al cliente.
        var pedidoDomain = pedidoRepository
                .buscarPorId(idPedidoGenerado)
                .orElseThrow(() -> new RuntimeException(
                        "Error al recuperar el pedido creado: " + idPedidoGenerado));

        // ── PASO 13: Enviar email de confirmación (falla silenciosa) ──────────
        // El email NO es crítico para el proceso de compra.
        // Si falla (SMTP caído, credenciales incorrectas, etc.), el pedido
        // ya está confirmado y el cliente puede verlo en "Mis pedidos".
        try {
            notificacionPort.enviarConfirmacionPedido(pedidoDomain);
            log.info("Email de confirmación enviado al cliente: {}", email);
        } catch (Exception e) {
            // Log de advertencia pero NO relanzamos la excepción
            // → @Transactional NO hace rollback → el pedido queda confirmado
            log.warn("No se pudo enviar email de confirmación al usuario {}: {}",
                    email, e.getMessage());
        }

        return pedidoDomain;
    }

    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Obtiene el historial de pedidos del cliente autenticado.
     *
     * Ordenados de más reciente a más antiguo (según PedidoJpaRepository).
     *
     * @param email correo del usuario (del JWT)
     * @return lista de pedidos con sus detalles y pagos
     */
    @Override
    public List<Pedido> obtenerHistorial(String email) {
        // Buscar usuario por email para obtener su ID
        var usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException(
                        "Usuario no encontrado: " + email));

        // Delegar al repositorio de dominio la consulta por idUsuario
        return pedidoRepository.buscarPorUsuario(usuario.getIdUsuario());
    }

    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Obtiene el detalle de un pedido específico del cliente.
     *
     * Incluye verificación de que el pedido pertenece al usuario solicitante
     * (un cliente no puede ver pedidos de otros clientes).
     *
     * @param email    correo del usuario autenticado
     * @param idPedido ID del pedido a consultar
     * @return pedido con detalles, pago y estado
     */
    @Override
    public Pedido obtenerDetalle(String email, Long idPedido) {
        // Verificar que el usuario existe
        var usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException(
                        "Usuario no encontrado: " + email));

        // Buscar el pedido por ID
        var pedido = pedidoRepository.buscarPorId(idPedido)
                .orElseThrow(() -> new RuntimeException(
                        "Pedido no encontrado: " + idPedido));

        // Validar que el pedido pertenece al usuario autenticado
        // (control de acceso: los clientes solo ven sus propios pedidos)
        if (!pedido.getIdUsuario().equals(usuario.getIdUsuario())) {
            throw new RuntimeException(
                    "No tienes permisos para ver este pedido");
        }

        return pedido;
    }
}