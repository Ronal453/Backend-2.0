package com.plantopolis.backend.infrastructure.persistence.repository;

import com.plantopolis.backend.infrastructure.persistence.entity.TareaRecurrenteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repositorio Spring Data JPA para la entidad TareaRecurrenteEntity (HU23).
 */
@Repository
public interface TareaRecurrenteJpaRepository extends JpaRepository<TareaRecurrenteEntity, Long> {

    /**
     * Lista todas las tareas recurrentes ordenadas cronológicamente por creación descendente.
     */
    List<TareaRecurrenteEntity> findAllByOrderByIdTareaRecurrenteDesc();

    /**
     * Busca tareas recurrentes activas cuya próxima ejecución debe procesarse a más tardar hoy.
     *
     * @param hoy Fecha actual de corte.
     * @return Lista de entidades recurrentes listas para generación de tareas.
     */
    @Query("""
        SELECT r FROM TareaRecurrenteEntity r
        WHERE r.activo = true
          AND r.fechaProximaEjecucion <= :hoy
        ORDER BY r.fechaProximaEjecucion ASC, r.idTareaRecurrente ASC
    """)
    List<TareaRecurrenteEntity> buscarActivasPendientes(@Param("hoy") LocalDate hoy);
}
