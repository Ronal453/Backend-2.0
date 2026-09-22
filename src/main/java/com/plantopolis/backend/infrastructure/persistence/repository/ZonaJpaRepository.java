package com.plantopolis.backend.infrastructure.persistence.repository;

import com.plantopolis.backend.infrastructure.persistence.entity.ZonaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ZonaJpaRepository extends JpaRepository<ZonaEntity, Long> {
    List<ZonaEntity> findByActivoTrueOrderByNombreAsc();
    List<ZonaEntity> findAllByOrderByNombreAsc();
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndIdZonaNot(String nombre, Long idZona);
}
