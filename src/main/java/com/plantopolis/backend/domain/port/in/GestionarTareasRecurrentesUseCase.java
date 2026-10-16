package com.plantopolis.backend.domain.port.in;

import com.plantopolis.backend.domain.model.TareaRecurrente;

import java.time.LocalDate;
import java.util.List;

/**
 * Caso de uso primario para la definición y generación automática de tareas recurrentes (HU23).
 */
public interface GestionarTareasRecurrentesUseCase {

    /**
     * Registra una nueva regla de tarea recurrente (HU23).
     *
     * @param definicion Parámetros de tipo, prioridad, frecuencia y objetivo (lote/zona).
     * @param fechaInicio Fecha inicial para el ciclo de ejecuciones.
     * @param emailAdmin Correo del administrador creador.
     * @return Tarea recurrente registrada.
     */
    TareaRecurrente crear(TareaRecurrente definicion, LocalDate fechaInicio, String emailAdmin);

    /**
     * Lista todas las definiciones de tareas periódicas configuradas en el vivero.
     *
     * @return Lista de tareas recurrentes ordenadas por fecha de creación descendente.
     */
    List<TareaRecurrente> listar();

    /**
     * Obtiene el detalle de una definición recurrente por su ID.
     *
     * @param id Identificador de la tarea recurrente.
     * @return Tarea recurrente encontrada.
     */
    TareaRecurrente buscarPorId(Long id);

    /**
     * Actualiza los parámetros de una tarea recurrente.
     *
     * @param id Identificador de la regla a modificar.
     * @param cambios Nuevos valores de frecuencia, prioridad, descripción, etc.
     * @return Tarea recurrente modificada.
     */
    TareaRecurrente actualizar(Long id, TareaRecurrente cambios);

    /**
     * Activa o desactiva la programación de una tarea recurrente.
     *
     * @param id Identificador de la regla.
     * @param activo {@code true} para habilitar; {@code false} para pausar.
     * @return Tarea recurrente con el estado de activación actualizado.
     */
    TareaRecurrente cambiarActivo(Long id, boolean activo);

    /**
     * Proceso por lotes que evalúa las tareas recurrentes pendientes de ejecución y genera
     * las instancias correspondientes en la tabla TAREA (HU23).
     *
     * @param hoy Fecha de corte actual (Bogotá).
     * @return Número de nuevas instancias generadas.
     */
    int generarInstanciasPendientes(LocalDate hoy);
}
