package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.*;
import com.plantopolis.backend.domain.port.in.GestionarTareasAdminUseCase;
import com.plantopolis.backend.domain.port.out.HistorialTareaRepositoryPort;
import com.plantopolis.backend.domain.port.out.TareaRepositoryPort;
import com.plantopolis.backend.domain.port.out.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de aplicación para las operaciones administrativas sobre tareas operativas (HU22, HU24, HU29, HU30).
 */
@Service
@RequiredArgsConstructor
public class AdminTareaService implements GestionarTareasAdminUseCase {

    private final TareaRepositoryPort tareaRepo;
    private final HistorialTareaRepositoryPort historialRepo;
    private final UsuarioRepositoryPort usuarioRepo;
    private final ValidadorTareas validadorTareas;

    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");

    @Override
    @Transactional
    public Tarea crearTarea(Tarea tarea, String emailAdmin) {
        if (tarea == null) {
            throw new IllegalArgumentException("Los datos de la tarea son obligatorios.");
        }

        var admin = usuarioRepo.buscarPorEmail(emailAdmin)
                .orElseThrow(() -> new RecursoNoEncontradoException("Administrador no encontrado con email: " + emailAdmin));

        LocalDate hoy = LocalDate.now(ZONA_BOGOTA);
        if (tarea.getFechaLimite() == null) {
            throw new IllegalArgumentException("La fecha límite de la tarea es obligatoria.");
        }
        if (tarea.getFechaLimite().isBefore(hoy)) {
            throw new IllegalArgumentException("La fecha límite no puede ser anterior a la fecha actual (" + hoy + ").");
        }

        // Validación exhaustiva de negocio (lote/zona XOR, activos, rol)
        validadorTareas.validarDefinicionTarea(
                tarea.getTipoTarea(),
                tarea.getPrioridad(),
                tarea.getIdZona(),
                tarea.getIdLote(),
                tarea.getIdTrabajadorAsignado()
        );

        tarea.setTipoTarea(TipoTarea.desde(tarea.getTipoTarea()).name());
        tarea.setPrioridad(PrioridadTarea.desde(tarea.getPrioridad()).name());
        tarea.setEstadoTarea(EstadoTarea.POR_HACER.name());
        tarea.setAlertaVencidaEnviada(false);
        tarea.setFechaCreacion(LocalDateTime.now(ZONA_BOGOTA));
        tarea.setFechaCompletada(null);

        if (tarea.getDescripcion() != null) {
            tarea.setDescripcion(tarea.getDescripcion().trim());
        }

        var tareaGuardada = tareaRepo.guardar(tarea);

        // Registro de auditoría inicial (tipoCambio = CREACION)
        var historial = HistorialTarea.builder()
                .idTarea(tareaGuardada.getIdTarea())
                .idUsuario(admin.getIdUsuario())
                .tipoCambio("CREACION")
                .estadoAnterior(null)
                .estadoNuevo(EstadoTarea.POR_HACER.name())
                .idTrabajadorAnterior(null)
                .idTrabajadorNuevo(tareaGuardada.getIdTrabajadorAsignado())
                .fechaCambio(LocalDateTime.now(ZONA_BOGOTA))
                .build();
        historialRepo.guardar(historial);

        return tareaGuardada;
    }

    @Override
    public List<Tarea> listarTareasAdmin(FiltroTareas filtro) {
        LocalDate hoy = LocalDate.now(ZONA_BOGOTA);
        return tareaRepo.buscarConFiltrosAdmin(filtro, hoy);
    }

    @Override
    @Transactional
    public Tarea reasignarTarea(Long idTarea, Long idTrabajadorNuevo, String emailAdmin) {
        var tarea = tareaRepo.buscarPorId(idTarea)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tarea no encontrada con ID: " + idTarea));

        if (EstadoTarea.COMPLETADA.name().equalsIgnoreCase(tarea.getEstadoTarea())) {
            throw new IllegalStateException("No es posible reasignar una tarea que ya ha sido completada.");
        }

        var admin = usuarioRepo.buscarPorEmail(emailAdmin)
                .orElseThrow(() -> new RecursoNoEncontradoException("Administrador no encontrado con email: " + emailAdmin));

        Long idAnterior = tarea.getIdTrabajadorAsignado();

        // Si la asignación no cambia, no se genera cambio innecesario
        if (Objects.equals(idAnterior, idTrabajadorNuevo)) {
            return tarea;
        }

        // Si se asigna a un nuevo trabajador, se valida que esté activo y con rol adecuado
        if (idTrabajadorNuevo != null) {
            validadorTareas.validarTrabajadorAsignado(idTrabajadorNuevo);
        }

        tarea.setIdTrabajadorAsignado(idTrabajadorNuevo);
        var tareaActualizada = tareaRepo.guardar(tarea);

        // Registro de auditoría (tipoCambio = REASIGNACION)
        var historial = HistorialTarea.builder()
                .idTarea(idTarea)
                .idUsuario(admin.getIdUsuario())
                .tipoCambio("REASIGNACION")
                .estadoAnterior(tarea.getEstadoTarea())
                .estadoNuevo(tarea.getEstadoTarea())
                .idTrabajadorAnterior(idAnterior)
                .idTrabajadorNuevo(idTrabajadorNuevo)
                .fechaCambio(LocalDateTime.now(ZONA_BOGOTA))
                .build();
        historialRepo.guardar(historial);

        return tareaActualizada;
    }

    @Override
    public ResumenTareas obtenerResumen(LocalDate fechaInicio, LocalDate fechaFin) {
        LocalDate hoy = LocalDate.now(ZONA_BOGOTA);
        LocalDate fin = (fechaFin != null) ? fechaFin : hoy;
        LocalDate inicio = (fechaInicio != null) ? fechaInicio : hoy.minusDays(29);

        if (inicio.isAfter(fin)) {
            throw new IllegalArgumentException("La fecha de inicio (" + inicio + ") no puede ser posterior a la fecha fin (" + fin + ").");
        }

        List<Tarea> todas = tareaRepo.buscarTodasParaResumen();
        List<Usuario> trabajadoresActivos = usuarioRepo.listarActivosPorRol(ValidadorTareas.ROL_TRABAJADOR);

        long totalActivas = 0;
        long vencidasActivas = 0;
        long completadasEnPeriodo = 0;
        long sinAsignar = 0;

        Map<String, Long> conteoPorEstado = new LinkedHashMap<>();
        for (EstadoTarea e : EstadoTarea.values()) {
            conteoPorEstado.put(e.name(), 0L);
        }

        List<Tarea> listaVencidas = new ArrayList<>();
        Map<LocalDate, Long> mapaCompletadasDia = new HashMap<>();

        for (Tarea t : todas) {
            String est = t.getEstadoTarea() != null ? t.getEstadoTarea().toUpperCase() : EstadoTarea.POR_HACER.name();
            conteoPorEstado.put(est, conteoPorEstado.getOrDefault(est, 0L) + 1);

            boolean esCompletada = EstadoTarea.COMPLETADA.name().equalsIgnoreCase(est);

            if (!esCompletada) {
                totalActivas++;
                if (t.getIdTrabajadorAsignado() == null) {
                    sinAsignar++;
                }
                if (t.getFechaLimite() != null && t.getFechaLimite().isBefore(hoy)) {
                    vencidasActivas++;
                    listaVencidas.add(t);
                }
            } else {
                if (t.getFechaCompletada() != null) {
                    LocalDate fechaComp = t.getFechaCompletada().toLocalDate();
                    if (!fechaComp.isBefore(inicio) && !fechaComp.isAfter(fin)) {
                        completadasEnPeriodo++;
                        mapaCompletadasDia.put(fechaComp, mapaCompletadasDia.getOrDefault(fechaComp, 0L) + 1);
                    }
                }
            }
        }

        // Estructura cronológica de completadas por día en el periodo
        List<ResumenTareas.PuntoSerieCompletadas> serieCompletadas = new ArrayList<>();
        LocalDate cursor = inicio;
        while (!cursor.isAfter(fin)) {
            serieCompletadas.add(new ResumenTareas.PuntoSerieCompletadas(
                    cursor,
                    mapaCompletadasDia.getOrDefault(cursor, 0L)
            ));
            cursor = cursor.plusDays(1);
        }

        // Carga operativa por trabajador
        Map<Long, List<Tarea>> tareasPorTrabajador = todas.stream()
                .filter(t -> t.getIdTrabajadorAsignado() != null)
                .collect(Collectors.groupingBy(Tarea::getIdTrabajadorAsignado));

        List<ResumenTareas.CargaTrabajador> cargaList = new ArrayList<>();
        for (Usuario trab : trabajadoresActivos) {
            List<Tarea> tareasTrab = tareasPorTrabajador.getOrDefault(trab.getIdUsuario(), Collections.emptyList());

            long ph = 0, ep = 0, er = 0, bl = 0, cp = 0, vc = 0;
            for (Tarea t : tareasTrab) {
                String est = t.getEstadoTarea() != null ? t.getEstadoTarea().toUpperCase() : "";
                if (EstadoTarea.POR_HACER.name().equals(est)) ph++;
                else if (EstadoTarea.EN_PROGRESO.name().equals(est)) ep++;
                else if (EstadoTarea.EN_REVISION.name().equals(est)) er++;
                else if (EstadoTarea.BLOQUEADA.name().equals(est)) bl++;
                else if (EstadoTarea.COMPLETADA.name().equals(est)) {
                    if (t.getFechaCompletada() != null) {
                        LocalDate fc = t.getFechaCompletada().toLocalDate();
                        if (!fc.isBefore(inicio) && !fc.isAfter(fin)) {
                            cp++;
                        }
                    }
                }

                if (!EstadoTarea.COMPLETADA.name().equals(est) && t.getFechaLimite() != null && t.getFechaLimite().isBefore(hoy)) {
                    vc++;
                }
            }

            cargaList.add(ResumenTareas.CargaTrabajador.builder()
                    .idTrabajador(trab.getIdUsuario())
                    .nombreTrabajador(trab.getNombreCompleto())
                    .emailTrabajador(trab.getCorreo())
                    .porHacer(ph)
                    .enProgreso(ep)
                    .enRevision(er)
                    .bloqueadas(bl)
                    .completadasPeriodo(cp)
                    .vencidas(vc)
                    .build());
        }

        cargaList.sort(Comparator.comparing(ResumenTareas.CargaTrabajador::getNombreTrabajador));
        listaVencidas.sort(Comparator.comparing(Tarea::getFechaLimite));

        return ResumenTareas.builder()
                .totalActivas(totalActivas)
                .vencidasActivas(vencidasActivas)
                .completadasEnPeriodo(completadasEnPeriodo)
                .sinAsignar(sinAsignar)
                .conteoPorEstado(conteoPorEstado)
                .cargaPorTrabajador(cargaList)
                .completadasPorDia(serieCompletadas)
                .tareasVencidas(listaVencidas)
                .build();
    }
}
