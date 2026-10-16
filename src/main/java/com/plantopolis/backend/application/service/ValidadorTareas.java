package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.*;
import com.plantopolis.backend.domain.port.out.LoteRepositoryPort;
import com.plantopolis.backend.domain.port.out.UsuarioRepositoryPort;
import com.plantopolis.backend.domain.port.out.ZonaRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Componente de validación transversal para las reglas de negocio de Tareas (HU22, HU23, HU30).
 * Aplica restricciones de exclusión mutua, integridad de objetivos y roles de asignación.
 */
@Component
@RequiredArgsConstructor
public class ValidadorTareas {

    private final ZonaRepositoryPort zonaRepo;
    private final LoteRepositoryPort loteRepo;
    private final UsuarioRepositoryPort usuarioRepo;

    public static final Long ROL_TRABAJADOR = 2L;

    /**
     * Valida la coherencia de una tarea o definición recurrente antes de su persistencia.
     *
     * @param tipoTarea Tipo de cuidado a realizar.
     * @param prioridad Nivel de prioridad.
     * @param idZona ID de zona (si aplica).
     * @param idLote ID de lote (si aplica).
     * @param idTrabajadorAsignado ID del trabajador (opcional).
     */
    public void validarDefinicionTarea(
            String tipoTarea,
            String prioridad,
            Long idZona,
            Long idLote,
            Long idTrabajadorAsignado
    ) {
        // 1. Validar tipo y prioridad según los enums de dominio
        TipoTarea.desde(tipoTarea);
        PrioridadTarea.desde(prioridad);

        // 2. Regla XOR lote / zona (HU22.3 / RF-22)
        validarObjetivoXor(idZona, idLote);

        // 3. Validar estado del objetivo (Zona activa o Lote no descartado)
        if (idZona != null) {
            var zona = zonaRepo.buscarPorId(idZona)
                    .orElseThrow(() -> new RecursoNoEncontradoException("La zona con ID " + idZona + " no existe."));
            if (Boolean.FALSE.equals(zona.getActivo())) {
                throw new IllegalStateException("No se pueden asignar tareas a la zona '" + zona.getNombre() + "' porque está inactiva.");
            }
        } else {
            var lote = loteRepo.buscarPorId(idLote)
                    .orElseThrow(() -> new RecursoNoEncontradoException("El lote con ID " + idLote + " no existe."));
            if ("DESCARTADO".equalsIgnoreCase(lote.getEstadoLote())) {
                throw new IllegalStateException("No se pueden asignar tareas al lote '" + lote.getCodigoLote() + "' porque está DESCARTADO.");
            }
        }

        // 4. Validar trabajador asignado si se especificó (HU22.4 / RF-22)
        if (idTrabajadorAsignado != null) {
            validarTrabajadorAsignado(idTrabajadorAsignado);
        }
    }

    /**
     * Valida que se cumpla la exclusión mutua exacta entre Zona y Lote.
     */
    public void validarObjetivoXor(Long idZona, Long idLote) {
        boolean tieneZona = idZona != null && idZona > 0;
        boolean tieneLote = idLote != null && idLote > 0;

        if (!tieneZona && !tieneLote) {
            throw new IllegalArgumentException("La tarea debe estar asociada obligatoriamente a una zona o a un lote de cultivo.");
        }
        if (tieneZona && tieneLote) {
            throw new IllegalArgumentException("La tarea no puede asociarse simultáneamente a una zona y a un lote (exclusión mutua).");
        }
    }

    /**
     * Valida que el usuario asignado exista, se encuentre activo y posea el rol TRABAJADOR (HU22, HU30).
     */
    public Usuario validarTrabajadorAsignado(Long idTrabajador) {
        var usuario = usuarioRepo.buscarPorId(idTrabajador)
                .orElseThrow(() -> new RecursoNoEncontradoException("El trabajador con ID " + idTrabajador + " no existe."));

        if (Boolean.FALSE.equals(usuario.getActivo())) {
            throw new IllegalStateException("El trabajador '" + usuario.getNombreCompleto() + "' se encuentra inactivo.");
        }

        boolean esTrabajador = ROL_TRABAJADOR.equals(usuario.getIdRol()) ||
                "TRABAJADOR".equalsIgnoreCase(usuario.getRolNombre());

        if (!esTrabajador) {
            throw new IllegalArgumentException("El usuario '" + usuario.getNombreCompleto() + "' no tiene rol TRABAJADOR.");
        }

        return usuario;
    }
}
