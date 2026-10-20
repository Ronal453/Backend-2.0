package com.plantopolis.backend.infrastructure.persistence.repository;

import com.plantopolis.backend.infrastructure.persistence.entity.TareaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repositorio Spring Data JPA para la entidad TareaEntity (HU22 - HU30).
 */
@Repository
public interface TareaJpaRepository extends JpaRepository<TareaEntity, Long> {

    /**
     * Filtra tareas operativas para el tablero del trabajador.
     * Usa CAST explícito a string para compatibilidad con PostgreSQL cuando los filtros son nulos.
     */
    @Query("""
        SELECT t FROM TareaEntity t
        WHERE (:idTrabajador IS NULL OR t.idTrabajadorAsignado = :idTrabajador)
          AND (CAST(:estado AS string) IS NULL OR UPPER(t.estadoTarea) = UPPER(CAST(:estado AS string)))
          AND (CAST(:prioridad AS string) IS NULL OR UPPER(t.prioridad) = UPPER(CAST(:prioridad AS string)))
        ORDER BY t.fechaLimite ASC, t.idTarea DESC
    """)
    List<TareaEntity> buscarConFiltros(
            @Param("idTrabajador") Long idTrabajador,
            @Param("estado") String estado,
            @Param("prioridad") String prioridad
    );

    /**
     * Filtra tareas para el panel administrativo con soporte de múltiples criterios opcionales.
     */
    @Query("""
        SELECT t FROM TareaEntity t
        WHERE (CAST(:estado AS string) IS NULL OR UPPER(t.estadoTarea) = UPPER(CAST(:estado AS string)))
          AND (CAST(:prioridad AS string) IS NULL OR UPPER(t.prioridad) = UPPER(CAST(:prioridad AS string)))
          AND (CAST(:tipo AS string) IS NULL OR UPPER(t.tipoTarea) = UPPER(CAST(:tipo AS string)))
          AND (:idTrabajador IS NULL OR t.idTrabajadorAsignado = :idTrabajador)
          AND (:sinAsignar IS NULL OR (:sinAsignar = true AND t.idTrabajadorAsignado IS NULL) OR (:sinAsignar = false AND t.idTrabajadorAsignado IS NOT NULL))
          AND (:soloVencidas IS NULL OR (:soloVencidas = true AND t.fechaLimite < :hoy AND t.estadoTarea <> 'COMPLETADA'))
          AND (:idZona IS NULL OR t.idZona = :idZona)
          AND (:idLote IS NULL OR t.idLote = :idLote)
        ORDER BY t.fechaLimite ASC, t.idTarea DESC
    """)
    List<TareaEntity> buscarConFiltrosAdmin(
            @Param("estado") String estado,
            @Param("prioridad") String prioridad,
            @Param("tipo") String tipo,
            @Param("idTrabajador") Long idTrabajador,
            @Param("sinAsignar") Boolean sinAsignar,
            @Param("soloVencidas") Boolean soloVencidas,
            @Param("hoy") LocalDate hoy,
            @Param("idZona") Long idZona,
            @Param("idLote") Long idLote
    );

    @Query("""
        SELECT t FROM TareaEntity t
        WHERE t.fechaLimite < :hoy
          AND t.estadoTarea <> 'COMPLETADA'
          AND (t.alertaVencidaEnviada = false OR t.alertaVencidaEnviada IS NULL)
        ORDER BY t.fechaLimite ASC
    """)
    List<TareaEntity> buscarVencidasSinAlerta(@Param("hoy") LocalDate hoy);

    boolean existsByIdTareaRecurrenteAndFechaLimite(Long idTareaRecurrente, LocalDate fechaLimite);

    @Modifying
    @Query("UPDATE TareaEntity t SET t.alertaVencidaEnviada = true WHERE t.idTarea IN :ids")
    void marcarAlertaVencidaEnviada(@Param("ids") List<Long> ids);

    @Query("SELECT t FROM TareaEntity t ORDER BY t.fechaCreacion DESC")
    List<TareaEntity> buscarTodasParaResumen();
}
