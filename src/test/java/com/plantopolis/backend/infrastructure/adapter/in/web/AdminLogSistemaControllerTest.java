package com.plantopolis.backend.infrastructure.adapter.in.web;

import com.plantopolis.backend.domain.model.LogSistema;
import com.plantopolis.backend.domain.model.ResumenLogsSistema;
import com.plantopolis.backend.domain.port.in.ConsultarLogsSistemaUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas unitarias para {@link AdminLogSistemaController}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminLogSistemaController — Endpoints de Logs del Sistema")
class AdminLogSistemaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ConsultarLogsSistemaUseCase logsUseCase;

    @InjectMocks
    private AdminLogSistemaController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/admin/logs retorna 200 con métricas y lista de acciones")
    void consultarLogs_retorna200() throws Exception {
        LogSistema item = LogSistema.builder()
                .id("ACT-1")
                .fechaHora(LocalDateTime.of(2026, 10, 15, 10, 0))
                .nivel("SUCCESS")
                .modulo("BACKUP")
                .accion("GENERAR_BACKUP_SQL")
                .descripcion("Copia de seguridad generada")
                .usuario("admin@plantopolis.com")
                .rol("ADMINISTRADOR")
                .metodoHttp("GET")
                .endpoint("/api/admin/backup/generar")
                .codigoEstado(200)
                .duracionMs(120L)
                .origen("API_HTTP")
                .build();

        ResumenLogsSistema resumen = ResumenLogsSistema.builder()
                .totalEventos(1)
                .totalSuccess(1)
                .eventosPorModulo(Map.of("BACKUP", 1L))
                .logs(List.of(item))
                .build();

        when(logsUseCase.consultarLogs(isNull(), eq("BACKUP"), isNull(), isNull(), eq(300)))
                .thenReturn(resumen);

        mockMvc.perform(get("/api/admin/logs").param("modulo", "BACKUP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEventos").value(1))
                .andExpect(jsonPath("$.totalSuccess").value(1))
                .andExpect(jsonPath("$.logs[0].accion").value("GENERAR_BACKUP_SQL"));
    }

    @Test
    @DisplayName("DELETE /api/admin/logs/memoria retorna 200 y limpia buffer")
    void limpiarLogsEnMemoria_retorna200() throws Exception {
        mockMvc.perform(delete("/api/admin/logs/memoria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").exists());

        verify(logsUseCase).limpiarLogsEnMemoria("ADMINISTRADOR");
    }
}
