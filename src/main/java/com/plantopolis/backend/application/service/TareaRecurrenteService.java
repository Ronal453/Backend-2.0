package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.*;
import com.plantopolis.backend.domain.port.in.GestionarTareasRecurrentesUseCase;
import com.plantopolis.backend.domain.port.out.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Servicio de aplicación para la definición y generación automática de tareas recurrentes (HU23).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TareaRecurrenteService implements GestionarTareasRecurrentesUseCase {

    private final TareaRecurrenteRepositoryPort recurrenteRepo;
    private final TareaRepositoryPort tareaRepo;
    private final HistorialTareaRepositoryPort historialRepo;
    private final LoteRepositoryPort loteRepo;
    private final ZonaRepositoryPort zonaRepo;
    private final UsuarioRepositoryPort usuarioRepo;
    private final ValidadorTareas validadorTareas;

    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");

    @Override
    @Transactional
    public TareaRecurrente crear(TareaRecurrente definicion, LocalDate fechaInicio, String emailAdmin) {
        if (definicion == null) {
            throw new IllegalArgumentException("La definición de la tarea recurrente es obligatoria.");
        }

        var admin = usuarioRepo.buscarPorEmail(emailAdmin)
                .orElseThrow(() -> new RecursoNoEncontradoException("Administrador no encontrado: " + emailAdmin));

        LocalDate hoy = LocalDate.now(ZONA_BOGOTA);
        LocalDate inicioEfectivo = (fechaInicio != null) ? fechaInicio : hoy;

        if (inicioEfectivo.isBefore(hoy)) {
            throw new IllegalArgumentException("La fecha de inicio de la recurrencia no puede ser anterior a hoy (" + hoy + ").");
        }

        if (definicion.getFrecuenciaDias() == null || definicion.getFrecuenciaDias() < 1) {
            throw new IllegalArgumentException("La frecuencia en días debe ser un entero positivo mayor o igual a 1.");
        }

        validadorTareas.validarDefinicionTarea(
                definicion.getTipoTarea(),
                definicion.getPrioridad(),
                definicion.getIdZona(),
                definicion.getIdLote(),
                definicion.getIdTrabajadorAsignado()
        );

        definicion.setTipoTarea(TipoTarea.desde(definicion.getTipoTarea()).name());
        definicion.setPrioridad(PrioridadTarea.desde(definicion.getPrioridad()).name());
        definicion.setActivo(true);
        definicion.setFechaProximaEjecucion(inicioEfectivo);
        definicion.setFechaCreacion(LocalDateTime.now(ZONA_BOGOTA));

        if (definicion.getDescripcion() != null) {
            definicion.setDescripcion(definicion.getDescripcion().trim());
        }

        var guardada = recurrenteRepo.guardarRecurrente(definicion);

        // Si la recurrencia inicia hoy, generamos inmediatamente la primera instancia
        if (inicioEfectivo.isEqual(hoy)) {
            generarInstanciaParaRegla(guardada, hoy, admin.getIdUsuario());
            // Avanzar fecha próxima ejecución
            guardada.setFechaProximaEjecucion(guardada.getFechaProximaEjecucion().plusDays(guardada.getFrecuenciaDias()));
            guardada = recurrenteRepo.guardarRecurrente(guardada);
        }

        return guardada;
    }

    @Override
    public List<TareaRecurrente> listar() {
        return recurrenteRepo.listarRecurrentes();
    }

    @Override
    public TareaRecurrente buscarPorId(Long id) {
        return recurrenteRepo.buscarRecurrentePorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tarea recurrente no encontrada con ID: " + id));
    }

    @Override
    @Transactional
    public TareaRecurrente actualizar(Long id, TareaRecurrente cambios) {
        var existente = buscarPorId(id);

        if (cambios.getFrecuenciaDias() != null && cambios.getFrecuenciaDias() < 1) {
            throw new IllegalArgumentException("La frecuencia debe ser mayor o igual a 1 día.");
        }

        Long zona = cambios.getIdZona();
        Long lote = cambios.getIdLote();
        if (zona == null && lote == null) {
            zona = existente.getIdZona();
            lote = existente.getIdLote();
        }
        Long trabajador = cambios.getIdTrabajadorAsignado();

        validadorTareas.validarDefinicionTarea(
                cambios.getTipoTarea() != null ? cambios.getTipoTarea() : existente.getTipoTarea(),
                cambios.getPrioridad() != null ? cambios.getPrioridad() : existente.getPrioridad(),
                zona,
                lote,
                trabajador
        );

        if (cambios.getTipoTarea() != null) existente.setTipoTarea(TipoTarea.desde(cambios.getTipoTarea()).name());
        if (cambios.getPrioridad() != null) existente.setPrioridad(PrioridadTarea.desde(cambios.getPrioridad()).name());
        if (cambios.getDescripcion() != null) existente.setDescripcion(cambios.getDescripcion().trim());
        if (cambios.getFrecuenciaDias() != null) existente.setFrecuenciaDias(cambios.getFrecuenciaDias());
        existente.setIdZona(zona);
        existente.setIdLote(lote);
        existente.setIdTrabajadorAsignado(trabajador);

        return recurrenteRepo.guardarRecurrente(existente);
    }

    @Override
    @Transactional
    public TareaRecurrente cambiarActivo(Long id, boolean activo) {
        var regla = buscarPorId(id);
        regla.setActivo(activo);

        LocalDate hoy = LocalDate.now(ZONA_BOGOTA);
        if (activo && regla.getFechaProximaEjecucion().isBefore(hoy)) {
            regla.setFechaProximaEjecucion(hoy);
        }

        return recurrenteRepo.guardarRecurrente(regla);
    }

    @Override
    @Transactional
    public int generarInstanciasPendientes(LocalDate hoy) {
        LocalDate fechaCorte = (hoy != null) ? hoy : LocalDate.now(ZONA_BOGOTA);
        List<TareaRecurrente> pendientes = recurrenteRepo.buscarRecurrentesActivasPendientes(fechaCorte);

        int generadas = 0;

        for (TareaRecurrente regla : pendientes) {
            try {
                // 1. Idempotencia: si ya existe una tarea generada para esta regla con fecha límite de hoy
                if (tareaRepo.existeInstanciaRecurrente(regla.getIdTareaRecurrente(), fechaCorte)) {
                    log.info("Instancia de tarea recurrente {} para fecha {} ya existe. Avanzando ciclo.", regla.getIdTareaRecurrente(), fechaCorte);
                    avanzarCiclo(regla, fechaCorte);
                    continue;
                }

                // 2. Verificar vigencia del objetivo
                if (regla.getIdLote() != null) {
                    var lote = loteRepo.buscarPorId(regla.getIdLote());
                    if (lote.isEmpty() || "DESCARTADO".equalsIgnoreCase(lote.get().getEstadoLote())) {
                        log.warn("Desactivando tarea recurrente {} porque su lote asociado fue descartado o eliminado.", regla.getIdTareaRecurrente());
                        regla.setActivo(false);
                        recurrenteRepo.guardarRecurrente(regla);
                        continue;
                    }
                } else if (regla.getIdZona() != null) {
                    var zona = zonaRepo.buscarPorId(regla.getIdZona());
                    if (zona.isEmpty() || Boolean.FALSE.equals(zona.get().getActivo())) {
                        log.warn("Desactivando tarea recurrente {} porque su zona asociada fue desactivada.", regla.getIdTareaRecurrente());
                        regla.setActivo(false);
                        recurrenteRepo.guardarRecurrente(regla);
                        continue;
                    }
                }

                // 3. Generar la tarea operativa
                generarInstanciaParaRegla(regla, fechaCorte, null);
                generadas++;

                // 4. Avanzar fecha de ejecución periódica
                avanzarCiclo(regla, fechaCorte);

            } catch (Exception ex) {
                log.error("Error al procesar tarea recurrente ID {}: {}", regla.getIdTareaRecurrente(), ex.getMessage(), ex);
            }
        }

        return generadas;
    }

    private void generarInstanciaParaRegla(TareaRecurrente regla, LocalDate fechaLimite, Long idAdminAuditoria) {
        Long trabajadorEfectivo = regla.getIdTrabajadorAsignado();
        if (trabajadorEfectivo != null) {
            var usuario = usuarioRepo.buscarPorId(trabajadorEfectivo);
            if (usuario.isEmpty() || Boolean.FALSE.equals(usuario.get().getActivo())) {
                log.info("Trabajador {} asignado a recurrencia {} está inactivo. La tarea nace en backlog.", trabajadorEfectivo, regla.getIdTareaRecurrente());
                trabajadorEfectivo = null;
            }
        }

        var nuevaTarea = Tarea.builder()
                .idTareaRecurrente(regla.getIdTareaRecurrente())
                .tipoTarea(regla.getTipoTarea())
                .prioridad(regla.getPrioridad())
                .descripcion(regla.getDescripcion())
                .estadoTarea(EstadoTarea.POR_HACER.name())
                .idZona(regla.getIdZona())
                .idLote(regla.getIdLote())
                .idTrabajadorAsignado(trabajadorEfectivo)
                .fechaLimite(fechaLimite)
                .alertaVencidaEnviada(false)
                .fechaCreacion(LocalDateTime.now(ZONA_BOGOTA))
                .fechaCompletada(null)
                .build();

        var guardada = tareaRepo.guardar(nuevaTarea);

        // Registro de auditoría
        Long idAutor = (idAdminAuditoria != null) ? idAdminAuditoria : 1L; // 1L = Admin sistema / proceso automático
        var historial = HistorialTarea.builder()
                .idTarea(guardada.getIdTarea())
                .idUsuario(idAutor)
                .tipoCambio("CREACION")
                .estadoAnterior(null)
                .estadoNuevo(EstadoTarea.POR_HACER.name())
                .idTrabajadorAnterior(null)
                .idTrabajadorNuevo(trabajadorEfectivo)
                .fechaCambio(LocalDateTime.now(ZONA_BOGOTA))
                .build();
        historialRepo.guardar(historial);
    }

    private void avanzarCiclo(TareaRecurrente regla, LocalDate fechaCorte) {
        LocalDate proxima = regla.getFechaProximaEjecucion();
        int salto = (regla.getFrecuenciaDias() != null && regla.getFrecuenciaDias() > 0) ? regla.getFrecuenciaDias() : 1;

        while (!proxima.isAfter(fechaCorte)) {
            proxima = proxima.plusDays(salto);
        }

        regla.setFechaProximaEjecucion(proxima);
        recurrenteRepo.guardarRecurrente(regla);
    }
}
