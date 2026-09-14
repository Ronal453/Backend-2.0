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
}
