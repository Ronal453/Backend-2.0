package com.plantopolis.backend.domain.port.out;

import com.plantopolis.backend.domain.model.TareaRecurrente;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Puerto secundario de persistencia para la definición de tareas recurrentes (HU23).
 */
public interface TareaRecurrenteRepositoryPort {

    /**
     * Guarda o actualiza una definición de tarea recurrente.
     *
     * @param recurrente Modelo de dominio con los datos a persistir.
     * @return Tarea recurrente guardada.
     */
    TareaRecurrente guardarRecurrente(TareaRecurrente recurrente);

    /**
     * Busca una tarea recurrente por su identificador único.
     *
     * @param id Identificador de la regla.
     * @return Tarea recurrente si existe.
     */
    Optional<TareaRecurrente> buscarRecurrentePorId(Long id);

    /**
     * Lista todas las tareas recurrentes registradas.
     *
     * @return Lista de reglas periódicas ordenadas por ID descendente.
     */
    List<TareaRecurrente> listarRecurrentes();

    /**
     * Obtiene las tareas recurrentes activas cuya próxima ejecución es menor o igual a la fecha de corte.
     *
     * @param hoy Fecha actual para evaluación del ciclo.
     * @return Lista de tareas recurrentes listas para generar instancia.
     */
    List<TareaRecurrente> buscarRecurrentesActivasPendientes(LocalDate hoy);
}
