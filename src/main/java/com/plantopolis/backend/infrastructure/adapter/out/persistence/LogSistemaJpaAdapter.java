package com.plantopolis.backend.infrastructure.adapter.out.persistence;

import com.plantopolis.backend.domain.model.LogSistema;
import com.plantopolis.backend.domain.port.out.LogSistemaRepositoryPort;
import com.plantopolis.backend.infrastructure.persistence.entity.ComentarioTareaEntity;
import com.plantopolis.backend.infrastructure.persistence.entity.HistorialEstadoLoteEntity;
import com.plantopolis.backend.infrastructure.persistence.entity.HistorialTareaEntity;
import com.plantopolis.backend.infrastructure.persistence.entity.LoteProduccionEntity;
import com.plantopolis.backend.infrastructure.persistence.entity.MermaEntity;
import com.plantopolis.backend.infrastructure.persistence.entity.PedidoEntity;
import com.plantopolis.backend.infrastructure.persistence.entity.UsuarioEntity;
import com.plantopolis.backend.infrastructure.persistence.repository.ComentarioTareaJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.repository.HistorialEstadoLoteJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.repository.HistorialTareaJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.repository.LoteJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.repository.MermaJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.repository.PedidoJpaRepository;
import com.plantopolis.backend.infrastructure.persistence.repository.UsuarioJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Adaptador de salida que implementa {@link LogSistemaRepositoryPort}.
 * Traduce los registros de auditoría de la base de datos y las acciones en vivo
 * a descripciones claras y comprensibles para cualquier usuario administrador.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogSistemaJpaAdapter implements LogSistemaRepositoryPort {

    private static final int MAX_BUFFER_TIEMPO_REAL = 500;
    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");

    private final HistorialTareaJpaRepository historialTareaRepo;
    private final ComentarioTareaJpaRepository comentarioTareaRepo;
    private final HistorialEstadoLoteJpaRepository historialLoteRepo;
    private final MermaJpaRepository mermaRepo;
    private final PedidoJpaRepository pedidoRepo;
    private final LoteJpaRepository loteRepo;
    private final UsuarioJpaRepository usuarioRepo;

    /** Buffer circular thread-safe para acciones ejecutadas en tiempo real. */
    private final Deque<LogSistema> bufferTiempoReal = new ConcurrentLinkedDeque<>();

    /** Secuencia atómica para identificadores internos. */
    private final AtomicLong secuenciaId = new AtomicLong(1000);

    /**
     * Almacena una nueva acción en el buffer de tiempo real.
     *
     * @param evento Objeto {@link LogSistema} a registrar.
     */
    @Override
    public void guardarLogEnTiempoReal(LogSistema evento) {
        if (evento == null) return;
        if (evento.getId() == null || evento.getId().isBlank()) {
            evento.setId("ACT-" + secuenciaId.incrementAndGet());
        }
        if (evento.getFechaHora() == null) {
            evento.setFechaHora(LocalDateTime.now(ZONA_BOGOTA));
        }
        bufferTiempoReal.addFirst(evento);
        while (bufferTiempoReal.size() > MAX_BUFFER_TIEMPO_REAL) {
            bufferTiempoReal.removeLast();
        }
    }

    /**
     * Devuelve las acciones capturadas en tiempo real durante la sesión actual.
     *
     * @return Lista de eventos en memoria.
     */
    @Override
    public List<LogSistema> obtenerLogsEnTiempoReal() {
        return new ArrayList<>(bufferTiempoReal);
    }

    /**
     * Limpia el buffer de eventos en tiempo real.
     */
    @Override
    public void limpiarBufferTiempoReal() {
        bufferTiempoReal.clear();
    }

    /**
     * Consulta las tablas históricas de la base de datos y transforma cada evento
     * en un mensaje claro y natural sobre qué acción se realizó en el sistema.
     *
     * @return Lista de eventos históricos convertidos a {@link LogSistema}.
     */
    @Override
    @Transactional(readOnly = true)
    public List<LogSistema> obtenerLogsAuditoriaBaseDatos() {
        List<LogSistema> resultado = new ArrayList<>();

        // 1. Cambios de estado y reasignaciones de Tareas
        try {
            List<HistorialTareaEntity> histTareas = historialTareaRepo.findAll(
                    PageRequest.of(0, 80, Sort.by(Sort.Direction.DESC, "fechaCambio"))
            ).getContent();

            for (HistorialTareaEntity h : histTareas) {
                String autor = h.getUsuario() != null ? h.getUsuario().getNombreCompleto() : "Usuario #" + h.getIdUsuario();
                String rol = (h.getUsuario() != null && h.getUsuario().getRol() != null)
                        ? h.getUsuario().getRol().getNombre() : "TRABAJADOR";
                String nivel = "BLOQUEADA".equalsIgnoreCase(h.getEstadoNuevo()) ? "WARN" : "SUCCESS";

                String tituloAccion;
                String desc;
                if ("REASIGNACION".equalsIgnoreCase(h.getTipoCambio())) {
                    tituloAccion = "Reasignación de tarea";
                    String ant = h.getTrabajadorAnterior() != null ? h.getTrabajadorAnterior().getNombreCompleto() : "Sin asignar";
                    String nvo = h.getTrabajadorNuevo() != null ? h.getTrabajadorNuevo().getNombreCompleto() : "Sin asignar";
                    desc = String.format("La tarea #%d pasó de estar asignada a %s para asignarse a %s.", h.getIdTarea(), ant, nvo);
                } else {
                    tituloAccion = "Actualización de tarea";
                    String estAnt = formatearTextoEstado(h.getEstadoAnterior() != null ? h.getEstadoAnterior() : "Inicio");
                    String estNvo = formatearTextoEstado(h.getEstadoNuevo());
                    desc = String.format("La tarea #%d cambió su estado de «%s» a «%s».", h.getIdTarea(), estAnt, estNvo);
                }

                resultado.add(LogSistema.builder()
                        .id("HTAREA-" + h.getIdHistorialTarea())
                        .fechaHora(h.getFechaCambio())
                        .nivel(nivel)
                        .modulo("TAREAS")
                        .accion(tituloAccion)
                        .descripcion(desc)
                        .usuario(autor)
                        .rol(rol)
                        .metodoHttp("PATCH")
                        .endpoint("/api/tareas/" + h.getIdTarea() + "/estado")
                        .codigoEstado(200)
                        .origen("AUDITORIA_BD")
                        .build());
            }
        } catch (Exception e) {
            log.debug("No se pudo consultar HISTORIALTAREA para logs: {}", e.getMessage());
        }

        // 2. Comentarios agregados a Tareas
        try {
            List<ComentarioTareaEntity> comentarios = comentarioTareaRepo.findAll(
                    PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "fechaCreacion"))
            ).getContent();

            for (ComentarioTareaEntity c : comentarios) {
                String autor = c.getUsuario() != null ? c.getUsuario().getNombreCompleto() : "Usuario #" + c.getIdUsuario();
                String rol = (c.getUsuario() != null && c.getUsuario().getRol() != null)
                        ? c.getUsuario().getRol().getNombre() : "TRABAJADOR";

                resultado.add(LogSistema.builder()
                        .id("CTAREA-" + c.getIdComentario())
                        .fechaHora(c.getFechaCreacion())
                        .nivel("INFO")
                        .modulo("TAREAS")
                        .accion("Nota agregada en tarea")
                        .descripcion(String.format("Se dejó un comentario en la tarea #%d: \"%s\"", c.getIdTarea(), truncar(c.getComentario(), 110)))
                        .usuario(autor)
                        .rol(rol)
                        .metodoHttp("POST")
                        .endpoint("/api/tareas/" + c.getIdTarea() + "/comentarios")
                        .codigoEstado(201)
                        .origen("AUDITORIA_BD")
                        .build());
            }
        } catch (Exception e) {
            log.debug("No se pudo consultar COMENTARIOTAREA para logs: {}", e.getMessage());
        }

        // 3. Cambios de fase en Lotes de Cultivo
        try {
            List<HistorialEstadoLoteEntity> histLotes = historialLoteRepo.findAll(
                    PageRequest.of(0, 60, Sort.by(Sort.Direction.DESC, "fechaCambio"))
            ).getContent();

            for (HistorialEstadoLoteEntity hl : histLotes) {
                String autor = hl.getUsuario() != null ? hl.getUsuario().getNombreCompleto() : "Usuario #" + hl.getIdUsuario();
                String rol = (hl.getUsuario() != null && hl.getUsuario().getRol() != null)
                        ? hl.getUsuario().getRol().getNombre() : "TRABAJADOR";
                String nivel = "DESCARTADO".equalsIgnoreCase(hl.getEstadoNuevo()) ? "WARN" : "SUCCESS";

                String obs = (hl.getObservaciones() != null && !hl.getObservaciones().isBlank())
                        ? " (" + truncar(hl.getObservaciones(), 90) + ")" : "";

                resultado.add(LogSistema.builder()
                        .id("HLOTE-" + hl.getIdHistorialLote())
                        .fechaHora(hl.getFechaCambio())
                        .nivel(nivel)
                        .modulo("LOTES")
                        .accion("Cambio de etapa en lote de cultivo")
                        .descripcion(String.format("El lote #%d pasó de «%s» a «%s»%s.",
                                hl.getIdLote(),
                                formatearTextoEstado(hl.getEstadoAnterior()),
                                formatearTextoEstado(hl.getEstadoNuevo()),
                                obs))
                        .usuario(autor)
                        .rol(rol)
                        .metodoHttp("PATCH")
                        .endpoint("/api/lotes/" + hl.getIdLote() + "/estado")
                        .codigoEstado(200)
                        .origen("AUDITORIA_BD")
                        .build());
            }
        } catch (Exception e) {
            log.debug("No se pudo consultar HISTORIALESTADOLOTE para logs: {}", e.getMessage());
        }

        // 4. Registro de Mermas (Pérdidas de plantas)
        try {
            List<MermaEntity> mermas = mermaRepo.findAll(
                    PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "fechaRegistro"))
            ).getContent();

            for (MermaEntity m : mermas) {
                String autor = m.getUsuario() != null ? m.getUsuario().getNombreCompleto() : "Usuario #" + m.getIdUsuario();
                String rol = (m.getUsuario() != null && m.getUsuario().getRol() != null)
                        ? m.getUsuario().getRol().getNombre() : "TRABAJADOR";
                String loteCod = m.getLote() != null ? m.getLote().getCodigoLote() : ("Lote #" + m.getIdLote());
                String causaNom = m.getCausa() != null ? m.getCausa().getNombreCausa() : ("Causa #" + m.getIdCausa());

                resultado.add(LogSistema.builder()
                        .id("MERMA-" + m.getIdMerma())
                        .fechaHora(m.getFechaRegistro())
                        .nivel("WARN")
                        .modulo("MERMAS")
                        .accion("Registro de pérdida (merma)")
                        .descripcion(String.format("Se reportaron %d plantas perdidas en el lote %s por motivo de «%s».",
                                m.getCantidadPerdida(), loteCod, causaNom))
                        .usuario(autor)
                        .rol(rol)
                        .metodoHttp("POST")
                        .endpoint("/api/mermas")
                        .codigoEstado(201)
                        .origen("AUDITORIA_BD")
                        .build());
            }
        } catch (Exception e) {
            log.debug("No se pudo consultar MERMA para logs: {}", e.getMessage());
        }

        // 5. Pedidos de clientes
        try {
            List<PedidoEntity> pedidos = pedidoRepo.findAll(
                    PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "fechaPedido"))
            ).getContent();

            for (PedidoEntity p : pedidos) {
                if (p.getFechaPedido() == null) continue;
                String cliente = p.getUsuario() != null ? p.getUsuario().getNombreCompleto() : "Cliente #" + p.getIdUsuario();
                String estadoPed = p.getEstado() != null ? p.getEstado().getDescripcionEstado() : "PENDIENTE";
                String nivel = "CANCELADO".equalsIgnoreCase(estadoPed) ? "WARN" : "SUCCESS";

                resultado.add(LogSistema.builder()
                        .id("PED-" + p.getIdPedido())
                        .fechaHora(p.getFechaPedido())
                        .nivel(nivel)
                        .modulo("PEDIDOS")
                        .accion("Compra realizada en tienda")
                        .descripcion(String.format("Pedido %s registrado por valor de $%s (Estado: %s).",
                                p.getNumeroPedido(), p.getTotal(), formatearTextoEstado(estadoPed)))
                        .usuario(cliente)
                        .rol("CLIENTE")
                        .metodoHttp("POST")
                        .endpoint("/api/pedidos/checkout")
                        .codigoEstado(201)
                        .origen("AUDITORIA_BD")
                        .build());
            }
        } catch (Exception e) {
            log.debug("No se pudo consultar PEDIDO para logs: {}", e.getMessage());
        }

        // 6. Creación de Lotes de Cultivo
        try {
            List<LoteProduccionEntity> lotes = loteRepo.findAll(
                    PageRequest.of(0, 40, Sort.by(Sort.Direction.DESC, "fechaCreacion"))
            ).getContent();

            for (LoteProduccionEntity l : lotes) {
                if (l.getFechaCreacion() == null) continue;
                String zonaNom = l.getZona() != null ? l.getZona().getNombre() : ("Zona #" + l.getIdZona());

                resultado.add(LogSistema.builder()
                        .id("LOTE-" + l.getIdLote())
                        .fechaHora(l.getFechaCreacion())
                        .nivel("SUCCESS")
                        .modulo("LOTES")
                        .accion("Nuevo lote sembrado")
                        .descripcion(String.format("Se creó el lote %s (%s) con %d unidades iniciales en %s.",
                                l.getCodigoLote(), l.getEspecie(), l.getCantidadInicial(), zonaNom))
                        .usuario("Administración")
                        .rol("ADMINISTRADOR")
                        .metodoHttp("POST")
                        .endpoint("/api/admin/lotes")
                        .codigoEstado(201)
                        .origen("AUDITORIA_BD")
                        .build());
            }
        } catch (Exception e) {
            log.debug("No se pudo consultar LOTEPRODUCCION para logs: {}", e.getMessage());
        }

        // 7. Registro de Usuarios y Trabajadores
        try {
            List<UsuarioEntity> usuarios = usuarioRepo.findAll(
                    PageRequest.of(0, 40, Sort.by(Sort.Direction.DESC, "fechaRegistro"))
            ).getContent();

            for (UsuarioEntity u : usuarios) {
                if (u.getFechaRegistro() == null) continue;
                String rolNom = u.getRol() != null ? u.getRol().getNombre() : "CLIENTE";
                String tituloAccion = "TRABAJADOR".equalsIgnoreCase(rolNom)
                        ? "Nuevo trabajador registrado"
                        : "Nuevo usuario registrado";

                resultado.add(LogSistema.builder()
                        .id("USR-" + u.getIdUsuario())
                        .fechaHora(u.getFechaRegistro())
                        .nivel(Boolean.TRUE.equals(u.getActivo()) ? "INFO" : "WARN")
                        .modulo("USUARIOS")
                        .accion(tituloAccion)
                        .descripcion(String.format("Se creó la cuenta de %s (%s) con perfil de %s.",
                                u.getNombreCompleto(), u.getCorreo(), formatearTextoEstado(rolNom)))
                        .usuario(u.getNombreCompleto())
                        .rol(rolNom)
                        .metodoHttp("POST")
                        .endpoint("/api/usuarios")
                        .codigoEstado(201)
                        .origen("AUDITORIA_BD")
                        .build());
            }
        } catch (Exception e) {
            log.debug("No se pudo consultar USUARIO para logs: {}", e.getMessage());
        }

        return resultado;
    }

    /**
     * Convierte códigos con guiones bajos (ej. LISTO_PARA_VENTA) a texto natural (ej. Listo para venta).
     */
    private String formatearTextoEstado(String estado) {
        if (estado == null || estado.isBlank()) return "";
        String limpio = estado.replace('_', ' ').toLowerCase(Locale.ROOT).trim();
        return Character.toUpperCase(limpio.charAt(0)) + limpio.substring(1);
    }

    /**
     * Trunca cadenas largas para mantener legibilidad.
     */
    private String truncar(String str, int max) {
        if (str == null) return "";
        String limpio = str.replaceAll("\\s+", " ").trim();
        return limpio.length() <= max ? limpio : limpio.substring(0, max) + "...";
    }
}
