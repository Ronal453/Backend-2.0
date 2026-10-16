package com.plantopolis.backend.domain.port.in;

import com.plantopolis.backend.domain.model.FiltroTareas;
import com.plantopolis.backend.domain.model.ResumenTareas;
import com.plantopolis.backend.domain.model.Tarea;

import java.time.LocalDate;
import java.util.List;

/**
 * Caso de uso primario para la gestión administrativa de tareas operativas (HU22, HU24, HU29, HU30).
 */
public interface GestionarTareasAdminUseCase {

    /**
     * Crea y programa una nueva tarea de vivero en estado POR_HACER (HU22).
     *
     * @param tarea Datos de la tarea a registrar.
     * @param emailAdmin Correo del administrador autenticado.
     * @return Tarea creada con su ID generado y auditoría inicial.
     */
    Tarea crearTarea(Tarea tarea, String emailAdmin);

    /**
     * Lista todas las tareas operativas con filtros combinables para el tablero del Administrador (HU24).
     *
     * @param filtro Criterios de filtrado por estado, prioridad, tipo, trabajador, etc.
     * @return Lista de tareas coincidentes.
     */
    List<Tarea> listarTareasAdmin(FiltroTareas filtro);

    /**
     * Reasigna una tarea a otro trabajador o la envía al backlog general (HU30).
     *
     * @param idTarea Identificador de la tarea.
     * @param idTrabajadorNuevo Identificador del nuevo trabajador asignado (o null para backlog).
     * @param emailAdmin Correo del administrador que ejecuta la reasignación.
     * @return Tarea actualizada con registro en el historial.
     */
    Tarea reasignarTarea(Long idTarea, Long idTrabajadorNuevo, String emailAdmin);

    /**
     * Genera el resumen consolidado de métricas operativas y carga de trabajo (HU29).
     *
     * @param fechaInicio Inicio del rango para análisis de tareas completadas (opcional).
     * @param fechaFin Fin del rango para análisis de tareas completadas (opcional).
     * @return Agregado con KPIs, conteos por estado, carga por operario y tareas vencidas.
     */
    ResumenTareas obtenerResumen(LocalDate fechaInicio, LocalDate fechaFin);
}
