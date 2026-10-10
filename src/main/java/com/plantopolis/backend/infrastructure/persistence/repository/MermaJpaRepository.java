package com.plantopolis.backend.infrastructure.persistence.repository;

import com.plantopolis.backend.infrastructure.persistence.entity.MermaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MermaJpaRepository extends JpaRepository<MermaEntity, Long> {

    @Query("SELECT m FROM MermaEntity m ORDER BY m.fechaRegistro DESC")
    List<MermaEntity> listarRecientes(Pageable pageable);

    @Query("SELECT m FROM MermaEntity m WHERE " +
           "(cast(:fechaInicio as date) IS NULL OR m.fechaMerma >= :fechaInicio) AND " +
           "(cast(:fechaFin as date) IS NULL OR m.fechaMerma <= :fechaFin) " +
           "ORDER BY m.fechaMerma DESC, m.fechaRegistro DESC")
    List<MermaEntity> buscarPorRangoFechas(@org.springframework.data.repository.query.Param("fechaInicio") java.time.LocalDate fechaInicio, 
                                           @org.springframework.data.repository.query.Param("fechaFin") java.time.LocalDate fechaFin);
}
