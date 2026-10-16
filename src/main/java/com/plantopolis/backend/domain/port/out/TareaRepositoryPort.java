package com.plantopolis.backend.domain.port.out;

import com.plantopolis.backend.domain.model.FiltroTareas;
import com.plantopolis.backend.domain.model.Tarea;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Puerto secundario de persistencia para el agregado de Tarea operativa.
 */
public interface TareaRepositoryPort {

    List<Tarea> buscarPorTrabajador(Long idTrabajador, String estado, String prioridad);

    List<Tarea> buscarTodas(String estado, String prioridad);

    Optional<Tarea> buscarPorId(Long idTarea);

    Tarea guardar(Tarea tarea);

    /**
     * Busca tareas con criterios administrativos avanzados (HU24, HU29).
     *
     * @param filtro Criterios combinables (estado, prioridad, tipo, trabajador, backlog, vencidas).
     * @param hoy Fecha de corte para cálculo de tareas vencidas.
     * @return Lista de tareas filtradas y ordenadas por fecha límite y prioridad.
     */
    List<Tarea> buscarConFiltrosAdmin(FiltroTareas filtro, LocalDate hoy);

    /**
     * Obtiene las tareas activas cuya fecha límite ya venció y aún no tienen alerta enviada (HU28).
     *
     * @param hoy Fecha de corte actual.
     * @return Tareas pendientes de alertar.
     */
    List<Tarea> buscarVencidasSinAlerta(LocalDate hoy);

    /**
     * Verifica si ya existe una tarea generada para una regla recurrente con una fecha límite específica.
     * Garantiza la idempotencia del proceso periódico de generación (HU23).
     *
     * @param idTareaRecurrente ID de la regla de recurrencia.
     * @param fechaLimite Fecha límite asignada a la instancia.
     * @return {@code true} si ya fue generada; {@code false} de lo contrario.
     */
    boolean existeInstanciaRecurrente(Long idTareaRecurrente, LocalDate fechaLimite);

    /**
     * Marca de forma masiva el indicador {@code alertaVencidaEnviada = 1} para evitar reenvíos (HU28).
     *
     * @param idsTareas Lista de identificadores de tareas notificadas.
     */
    void marcarAlertaVencidaEnviada(List<Long> idsTareas);

    /**
     * Obtiene todas las tareas del sistema para el cálculo en memoria de métricas operativas (HU29).
     *
     * @return Lista completa de tareas.
     */
    List<Tarea> buscarTodasParaResumen();
}
