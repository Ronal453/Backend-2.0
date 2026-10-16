package com.plantopolis.backend.infrastructure.adapter.in.scheduler;

import com.plantopolis.backend.domain.port.in.GestionarTareasRecurrentesUseCase;
import com.plantopolis.backend.domain.port.in.NotificarTareasVencidasUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Programador de tareas en segundo plano para el ciclo de vida operativo (HU23 y HU28).
 * Ejecuta la generación automática de tareas periódicas y el envío diario de alertas por vencimiento.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TareasScheduler {

    private final GestionarTareasRecurrentesUseCase recurrentesUseCase;
    private final NotificarTareasVencidasUseCase notificarVencidasUseCase;

    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");

    /**
     * Proceso diario (00:05 AM hora de Bogotá) que evalúa las tareas recurrentes
     * y genera las instancias de trabajo para la fecha actual (HU23).
     */
    @Scheduled(cron = "${tareas.cron.recurrentes:0 5 0 * * *}", zone = "America/Bogota")
    public void ejecutarGeneracionRecurrentes() {
        log.info("⏰ [CRON] Iniciando generación automática de tareas recurrentes...");
        try {
            int generadas = recurrentesUseCase.generarInstanciasPendientes(LocalDate.now(ZONA_BOGOTA));
            log.info("⏰ [CRON] Generación de recurrentes finalizada. Instancias creadas: {}", generadas);
        } catch (Exception e) {
            log.error("❌ [CRON] Error al ejecutar generación de tareas recurrentes: {}", e.getMessage(), e);
        }
    }

    /**
     * Proceso diario (07:00 AM hora de Bogotá) que detecta tareas activas cuya fecha
     * límite expiró y notifica por correo electrónico a los administradores (HU28).
     */
    @Scheduled(cron = "${tareas.cron.alertas-vencidas:0 0 7 * * *}", zone = "America/Bogota")
    public void ejecutarAlertaTareasVencidas() {
        log.info("⏰ [CRON] Iniciando verificación y alerta de tareas vencidas...");
        try {
            int notificadas = notificarVencidasUseCase.notificarTareasVencidas(LocalDate.now(ZONA_BOGOTA));
            log.info("⏰ [CRON] Verificación de vencidas finalizada. Tareas alertadas: {}", notificadas);
        } catch (Exception e) {
            log.error("❌ [CRON] Error al ejecutar alerta de tareas vencidas: {}", e.getMessage(), e);
        }
    }
}
