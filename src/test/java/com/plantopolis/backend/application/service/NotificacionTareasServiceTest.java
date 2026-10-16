package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.Tarea;
import com.plantopolis.backend.domain.model.Usuario;
import com.plantopolis.backend.domain.port.out.NotificacionPort;
import com.plantopolis.backend.domain.port.out.TareaRepositoryPort;
import com.plantopolis.backend.domain.port.out.UsuarioRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificacionTareasService")
class NotificacionTareasServiceTest {

    @Mock private TareaRepositoryPort tareaRepo;
    @Mock private UsuarioRepositoryPort usuarioRepo;
    @Mock private NotificacionPort notificacionPort;

    @InjectMocks
    private NotificacionTareasService sut;

    private Usuario adminDemo() {
        return Usuario.builder()
                .idUsuario(1L)
                .nombreCompleto("Administrador")
                .correo("admin@plantopolis.com")
                .idRol(1L)
                .build();
    }

    private Tarea tareaVencida() {
        return Tarea.builder()
                .idTarea(55L)
                .tipoTarea("PODA")
                .prioridad("ALTA")
                .fechaLimite(LocalDate.now().minusDays(2))
                .alertaVencidaEnviada(false)
                .build();
    }

    @Test
    @DisplayName("Envía email y marca alertas cuando hay vencidas y administradores activos")
    void notificar_conVencidasYAdmin_enviaYMarca() {
        LocalDate hoy = LocalDate.now();
        when(tareaRepo.buscarVencidasSinAlerta(hoy)).thenReturn(List.of(tareaVencida()));
        when(usuarioRepo.listarActivosPorRol(1L)).thenReturn(List.of(adminDemo()));
        when(notificacionPort.enviarAlertaTareasVencidas(eq("admin@plantopolis.com"), any(), any())).thenReturn(true);

        // Act
        int resultado = sut.notificarTareasVencidas(hoy);

        // Assert
        assertThat(resultado).isEqualTo(1);
        verify(notificacionPort).enviarAlertaTareasVencidas(eq("admin@plantopolis.com"), eq("Administrador"), any());
        verify(tareaRepo).marcarAlertaVencidaEnviada(List.of(55L));
    }

    @Test
    @DisplayName("Si el envío de email falla para todos los administradores, no marca la alerta")
    void notificar_falloEnvio_noMarcaAlerta() {
        LocalDate hoy = LocalDate.now();
        when(tareaRepo.buscarVencidasSinAlerta(hoy)).thenReturn(List.of(tareaVencida()));
        when(usuarioRepo.listarActivosPorRol(1L)).thenReturn(List.of(adminDemo()));
        when(notificacionPort.enviarAlertaTareasVencidas(any(), any(), any())).thenReturn(false);

        // Act
        int resultado = sut.notificarTareasVencidas(hoy);

        // Assert
        assertThat(resultado).isEqualTo(0);
        verify(tareaRepo, never()).marcarAlertaVencidaEnviada(any());
    }

    @Test
    @DisplayName("Si no hay tareas vencidas, no contacta a administradores ni marca alertas")
    void notificar_sinVencidas_noHaceNada() {
        LocalDate hoy = LocalDate.now();
        when(tareaRepo.buscarVencidasSinAlerta(hoy)).thenReturn(Collections.emptyList());

        // Act
        int resultado = sut.notificarTareasVencidas(hoy);

        // Assert
        assertThat(resultado).isEqualTo(0);
        verify(usuarioRepo, never()).listarActivosPorRol(any());
        verify(notificacionPort, never()).enviarAlertaTareasVencidas(any(), any(), any());
    }
}
