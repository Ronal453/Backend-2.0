package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.Pago;
import com.plantopolis.backend.domain.model.Pedido;
import com.plantopolis.backend.domain.port.in.GestionarPedidosAdminUseCase;
import com.plantopolis.backend.domain.port.out.NotificacionPort;
import com.plantopolis.backend.domain.port.out.ProductoRepositoryPort;
import com.plantopolis.backend.infrastructure.persistence.entity.PedidoEntity;
import com.plantopolis.backend.infrastructure.persistence.repository.EstadoPedidoJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.repository.MetodoPagoJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.repository.PagoJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.repository.PedidoJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.mapper.PedidoMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPedidoService implements GestionarPedidosAdminUseCase {

    private final PedidoJpaRepository       pedidoJpaRepo;
    private final EstadoPedidoJpaRepository estadoPedidoRepo;
    private final ProductoRepositoryPort    productoRepository;
    private final NotificacionPort          notificacionPort;
    private final PedidoMapper              pedidoMapper;

    // ✅ FIX: repositorios para enriquecer el método de pago
    private final PagoJpaRepository      pagoJpaRepo;
    private final MetodoPagoJpaRepository metodoPagoRepo;

    // EntityManager para limpiar caché L1 de Hibernate (fix emails atrasados)
    @PersistenceContext
    private EntityManager entityManager;

    // ID del método EFECTIVO = Contra entrega (estado pago = PENDIENTE)
    private static final Long ID_METODO_EFECTIVO = 4L;

    // Mapa de transiciones válidas entre estados
    private static final Map<String, List<String>> TRANSICIONES_VALIDAS = Map.of(
    "PENDIENTE",       List.of("EN_PREPARACION", "CANCELADO"),
    "EN_PREPARACION",  List.of("ENVIADO",         "CANCELADO"),
    "ENVIADO",         List.of("ENTREGADO",       "CANCELADO"),
    "ENTREGADO",       List.of(),
    "CANCELADO",       List.of()
);

    // ── Listar todos los pedidos ──────────────────────────────────────────
    @Override
    public Page<Pedido> listarTodos(String estado, Pageable pageable) {
        String estadoFiltro = (estado != null && !estado.isBlank())
                ? estado.toUpperCase().trim()
                : null;

        // 1. Obtener la página de pedidos mapeados al dominio
        Page<Pedido> pagina = pedidoJpaRepo
                .findAllAdminWithFilters(estadoFiltro, pageable)
                .map(pedidoMapper::toDomain);

        // 2. ✅ FIX: enriquecer el pago de cada pedido cargando directamente
        //    desde la BD por ID, sin depender de las relaciones @ManyToOne
        //    que pueden quedar null por caché de Hibernate
        pagina.forEach(this::enriquecerPago);

        return pagina;
    }

    // ── Actualizar estado de un pedido ────────────────────────────────────
    @Override
    @Transactional
    public Pedido actualizarEstado(Long idPedido, String nuevoEstado) {

        // PASO 1: Cargar con JOIN FETCH para tener detalles (necesario para stock)
        var pedidoEntity = pedidoJpaRepo
                .findByIdWithRelations(idPedido)
                .orElseThrow(() -> new RuntimeException(
                        "Pedido no encontrado: " + idPedido));

        String estadoActual = pedidoEntity.getEstado() != null
                ? pedidoEntity.getEstado().getDescripcionEstado()
                : "DESCONOCIDO";

        String nuevoEstadoUpper = nuevoEstado.toUpperCase().trim();

        log.info("Admin: cambiando pedido {} de '{}' a '{}'",
                idPedido, estadoActual, nuevoEstadoUpper);

        // PASO 2: Validar transición
        List<String> permitidos =
                TRANSICIONES_VALIDAS.getOrDefault(estadoActual, List.of());

        if (!permitidos.contains(nuevoEstadoUpper)) {
            throw new RuntimeException(String.format(
                    "Transición no permitida: '%s' → '%s'. Válidos: %s",
                    estadoActual, nuevoEstadoUpper, permitidos));
        }

        // PASO 3: Si es CANCELADO → restaurar stock ANTES del clear()
        if ("CANCELADO".equals(nuevoEstadoUpper)) {
            restaurarStock(pedidoEntity);
        }

        // PASO 4: Buscar ID del nuevo estado por descripción
        var nuevoEstadoEntity = estadoPedidoRepo
                .findByDescripcionEstado(nuevoEstadoUpper)
                .orElseThrow(() -> new RuntimeException(
                        "Estado no encontrado en BD: " + nuevoEstadoUpper));

        
        // PASO 5: Persistir el cambio de estado
        pedidoEntity.setIdEstado(nuevoEstadoEntity.getIdEstadoPedido()); 
        pedidoJpaRepo.saveAndFlush(pedidoEntity);

        // PASO 6: Limpiar caché L1 para que la recarga lea BD fresca
        entityManager.clear();

        // PASO 7: Recargar desde BD con el estado actualizado
        var pedidoActualizado = pedidoJpaRepo
                .findByIdWithRelations(idPedido)
                .map(pedidoMapper::toDomain)
                .orElseThrow();

        // PASO 8: Enriquecer el pago del pedido recargado
        enriquecerPago(pedidoActualizado);

        log.info("Pedido {} → estado '{}'", idPedido,
                pedidoActualizado.getEstadoDescripcion());

        // PASO 9: Enviar email al cliente (silencioso si falla)
        try {
            notificacionPort.enviarCambioDeEstado(pedidoActualizado);
            log.info("✅ Email '{}' enviado a: {}",
                    nuevoEstadoUpper, pedidoActualizado.getEmailCliente());
        } catch (Exception e) {
            log.warn("⚠ Email falló (pedido {}): {}", idPedido, e.getMessage());
        }

        return pedidoActualizado;
    }

    // ── enriquecerPago: carga el método de pago directo desde BD ─────────
    /**
     * Enriquece el objeto Pago del pedido con nombreMetodo y estadoPago.
     *
     * Estrategia (mismo patrón que PedidoService):
     *   1. findByIdPedido() → obtiene el PagoEntity por idPedido (query simple)
     *   2. findById(idMetodo) → obtiene el nombre del método (query simple)
     *   3. Determina estadoPago: EFECTIVO → PENDIENTE, otros → APROBADO
     *   4. Asigna los valores al dominio Pedido
     *
     * No usa relaciones JPA (@ManyToOne) para evitar problemas de caché
     * y asegurar que siempre se leen los datos frescos de la BD.
     *
     * @param pedido pedido del dominio al que enriquecer el pago
     */
    private void enriquecerPago(Pedido pedido) {
        if (pedido == null || pedido.getIdPedido() == null) return;

        // Si el pago ya tiene nombreMetodo cargado, no hacer nada
        if (pedido.getPago() != null
                && pedido.getPago().getNombreMetodo() != null
                && !pedido.getPago().getNombreMetodo().isBlank()) {
            return;
        }

        // Buscar el PagoEntity directamente por idPedido (no por relación)
        var pagoOpt = pagoJpaRepo.findByIdPedido(pedido.getIdPedido());
        if (pagoOpt.isEmpty()) {
            log.warn("No se encontró pago para pedido {}", pedido.getIdPedido());
            return;
        }

        var pagoEntity = pagoOpt.get();
        Long idMetodo  = pagoEntity.getIdMetodo();

        if (idMetodo == null) return;

        // Buscar el nombre del método directamente por su ID
        String nombreMetodo = metodoPagoRepo.findById(idMetodo)
                .map(m -> m.getNombreMetodo())
                .orElse("DESCONOCIDO");

        // EFECTIVO (id=4) = Contra entrega → PENDIENTE hasta recibir
        // Todos los demás = pago electrónico → APROBADO
        String estadoPago = ID_METODO_EFECTIVO.equals(idMetodo)
                ? "PENDIENTE" : "APROBADO";

        log.debug("Pago enriquecido — pedido {}: método='{}', estado='{}'",
                pedido.getIdPedido(), nombreMetodo, estadoPago);

        // Actualizar o crear el objeto Pago en el dominio
        if (pedido.getPago() != null) {
            pedido.getPago().setNombreMetodo(nombreMetodo);
            pedido.getPago().setEstadoPagoDescripcion(estadoPago);
            pedido.getPago().setIdMetodo(idMetodo);
            if (pedido.getPago().getMonto() == null) {
                pedido.getPago().setMonto(pagoEntity.getMonto());
            }
        } else {
            pedido.setPago(Pago.builder()
                    .idPago(pagoEntity.getIdPago())
                    .idPedido(pedido.getIdPedido())
                    .idMetodo(idMetodo)
                    .monto(pagoEntity.getMonto())
                    .fechaPago(pagoEntity.getFechaPago())
                    .nombreMetodo(nombreMetodo)
                    .estadoPagoDescripcion(estadoPago)
                    .build());
        }
    }

    // ── restaurarStock: devuelve stock al cancelar ────────────────────────
    /**
     * Restaura el stock de cada producto del pedido cancelado.
     * Debe llamarse ANTES de entityManager.clear().
     */
    private void restaurarStock(PedidoEntity pedidoEntity) {
        log.info("Restaurando stock del pedido: {}", pedidoEntity.getIdPedido());
        if (pedidoEntity.getDetalles() == null) return;

        pedidoEntity.getDetalles().forEach(detalle ->
            productoRepository.buscarPorId(detalle.getIdProducto())
                    .ifPresent(producto -> {
                        int stockNuevo = producto.getStock() + detalle.getCantidad();
                        producto.setStock(stockNuevo);
                        productoRepository.guardar(producto);
                        log.debug("Stock restaurado: producto {} → {} ud.",
                                detalle.getIdProducto(), stockNuevo);
                    })
        );
    }
}