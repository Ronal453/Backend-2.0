package com.plantopolis.backend.infrastructure.persistence.repository;

import com.plantopolis.backend.infrastructure.persistence.entity.PagoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Optional;

public interface PagoJpaRepository extends JpaRepository<PagoEntity, Long> {

    /** Busca el pago de un pedido específico (Sprint 4). */
    Optional<PagoEntity> findByIdPedido(Long idPedido);

    /**
     * Suma el monto de todos los pagos con estado APROBADO (idEstadoPago = 2).
     *
     * COALESCE(SUM(...), 0): si no hay pagos aprobados, devuelve 0 en lugar de null
     * para evitar NullPointerException en el servicio de reportes.
     *
     * @return total de ingresos aprobados en la moneda de la BD
     */
    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM PagoEntity p WHERE p.idEstadoPago = 2")
    BigDecimal getTotalIngresosAprobados();
}