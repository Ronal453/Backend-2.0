package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.LogSistema;
import com.plantopolis.backend.domain.model.ResumenLogsSistema;
import com.plantopolis.backend.domain.port.out.LogSistemaRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para {@link AdminLogSistemaService}.
 * Valida la consolidación de eventos en tiempo real con la auditoría de base de datos,
 * el cálculo de métricas por nivel/módulo y la aplicación de filtros.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminLogSistemaService — Logs del Sistema")
class AdminLogSistemaServiceTest {

    @Mock
    private LogSistemaRepositoryPort logRepositoryPort;

    @InjectMocks
    private AdminLogSistemaService service;

    @Test
    @DisplayName("Consolida logs de memoria y BD, calcula métricas y filtra por módulo y búsqueda")
    void consultarLogs_filtraYCalculaMetricas() {
        LogSistema logTiempoReal = LogSistema.builder()
                .id("ACT-1001")
                .fechaHora(LocalDateTime.of(2026, 10, 15, 14, 30))
                .nivel("SUCCESS")
                .modulo("AUTH")
                .accion("INICIO_SESION")
                .descripcion("Inicio de sesión exitoso")
                .usuario("admin@plantopolis.com")
                .rol("ADMINISTRADOR")
                .origen("API_HTTP")
                .build();

        LogSistema logAuditoria = LogSistema.builder()
                .id("HTAREA-10")
                .fechaHora(LocalDateTime.of(2026, 10, 15, 12, 0))
                .nivel("WARN")
                .modulo("TAREAS")
                .accion("CAMBIO_ESTADO_TAREA")
                .descripcion("Tarea #10 bloqueada por falta de sustrato")
                .usuario("Operario Juan")
                .rol("TRABAJADOR")
                .origen("AUDITORIA_BD")
                .build();

        when(logRepositoryPort.obtenerLogsEnTiempoReal()).thenReturn(List.of(logTiempoReal));
        when(logRepositoryPort.obtenerLogsAuditoriaBaseDatos()).thenReturn(List.of(logAuditoria));

        ResumenLogsSistema res = service.consultarLogs("sustrato", "TAREAS", null, null, 100);

        assertEquals(2, res.getTotalEventos());
        assertEquals(1, res.getTotalSuccess());
        assertEquals(1, res.getTotalWarn());
        assertEquals(1, res.getLogs().size());
        assertEquals("HTAREA-10", res.getLogs().get(0).getId());
    }

    @Test
    @DisplayName("Limpiar logs en memoria reinicia el buffer y registra evento de auditoría")
    void limpiarLogsEnMemoria_reiniciaYRegistraEvento() {
        service.limpiarLogsEnMemoria("admin@plantopolis.com");
        verify(logRepositoryPort).limpiarBufferTiempoReal();
        verify(logRepositoryPort).guardarLogEnTiempoReal(any(LogSistema.class));
    }
}
