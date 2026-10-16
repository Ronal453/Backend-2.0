package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.*;
import com.plantopolis.backend.domain.port.out.ComentarioTareaRepositoryPort;
import com.plantopolis.backend.domain.port.out.HistorialTareaRepositoryPort;
import com.plantopolis.backend.domain.port.out.TareaRepositoryPort;
import com.plantopolis.backend.domain.port.out.UsuarioRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrabajadorTareaService")
class TrabajadorTareaServiceTest {

    @Mock private TareaRepositoryPort tareaRepo;
    @Mock private HistorialTareaRepositoryPort historialRepo;
    @Mock private ComentarioTareaRepositoryPort comentarioRepo;
    @Mock private UsuarioRepositoryPort usuarioRepo;

    @InjectMocks
    private TrabajadorTareaService sut;

    private Usuario trabajadorDemo() {
        return Usuario.builder()
                .idUsuario(10L)
                .nombreCompleto("Juan Operario")
                .correo("operario@plantopolis.com")
                .idRol(2L)
                .rolNombre("TRABAJADOR")
                .build();
    }

    private Usuario adminDemo() {
        return Usuario.builder()
                .idUsuario(1L)
                .nombreCompleto("Admin General")
                .correo("admin@plantopolis.com")
                .idRol(1L)
                .rolNombre("ADMINISTRADOR")
                .build();
    }

    private Tarea tareaPorHacer() {
        return Tarea.builder()
                .idTarea(100L)
                .tipoTarea("RIEGO")
                .prioridad("ALTA")
                .estadoTarea("POR_HACER")
                .idTrabajadorAsignado(10L)
                .fechaLimite(LocalDate.now().plusDays(2))
                .alertaVencidaEnviada(false)
                .build();
    }

    @Nested
    @DisplayName("cambiarEstadoTarea()")
    class CambiarEstadoTests {

        @Test
        @DisplayName("Transición válida POR_HACER -> EN_PROGRESO registra historial")
        void transicionValida_debeActualizarYRegistrarHistorial() {
            // Arrange
            when(tareaRepo.buscarPorId(100L)).thenReturn(Optional.of(tareaPorHacer()));
            when(usuarioRepo.buscarPorEmail("operario@plantopolis.com")).thenReturn(Optional.of(trabajadorDemo()));
            when(tareaRepo.guardar(any(Tarea.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Tarea actualizada = sut.cambiarEstadoTarea(100L, "EN_PROGRESO", "Iniciando riego", "operario@plantopolis.com");

            // Assert
            assertThat(actualizada.getEstadoTarea()).isEqualTo("EN_PROGRESO");
            verify(historialRepo).guardar(any(HistorialTarea.class));
            verify(comentarioRepo).guardar(any(ComentarioTarea.class));
        }

        @Test
        @DisplayName("Transición inválida POR_HACER -> COMPLETADA lanza excepción")
        void transicionInvalida_debeLanzarExcepcion() {
            // Arrange
            when(tareaRepo.buscarPorId(100L)).thenReturn(Optional.of(tareaPorHacer()));
            when(usuarioRepo.buscarPorEmail("operario@plantopolis.com")).thenReturn(Optional.of(trabajadorDemo()));

            // Act & Assert
            assertThatThrownBy(() -> sut.cambiarEstadoTarea(100L, "COMPLETADA", null, "operario@plantopolis.com"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Transición no permitida");

            verify(tareaRepo, never()).guardar(any());
            verify(historialRepo, never()).guardar(any());
        }

        @Test
        @DisplayName("Permite bloquear tarea activa a BLOQUEADA")
        void bloquearTarea_debePermitirTransicionABloqueada() {
            // Arrange
            when(tareaRepo.buscarPorId(100L)).thenReturn(Optional.of(tareaPorHacer()));
            when(usuarioRepo.buscarPorEmail("operario@plantopolis.com")).thenReturn(Optional.of(trabajadorDemo()));
            when(tareaRepo.guardar(any(Tarea.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Tarea bloqueada = sut.cambiarEstadoTarea(100L, "BLOQUEADA", "Sin manguera disponible", "operario@plantopolis.com");

            // Assert
            assertThat(bloqueada.getEstadoTarea()).isEqualTo("BLOQUEADA");
            verify(historialRepo).guardar(any(HistorialTarea.class));
        }

        @Test
        @DisplayName("Trabajador intentando mover tarea asignada a otro trabajador lanza OperacionNoPermitidaException (403)")
        void trabajadorMueveTareaAjena_debeLanzarOperacionNoPermitida() {
            // Arrange: Tarea asignada al operario 99L, pero intenta moverla el operario 10L
            Tarea tareaAjena = tareaPorHacer();
            tareaAjena.setIdTrabajadorAsignado(99L);

            when(tareaRepo.buscarPorId(100L)).thenReturn(Optional.of(tareaAjena));
            when(usuarioRepo.buscarPorEmail("operario@plantopolis.com")).thenReturn(Optional.of(trabajadorDemo()));

            // Act & Assert
            assertThatThrownBy(() -> sut.cambiarEstadoTarea(100L, "EN_PROGRESO", null, "operario@plantopolis.com"))
                    .isInstanceOf(OperacionNoPermitidaException.class)
                    .hasMessageContaining("No tienes permisos para modificar el estado de una tarea asignada a otro trabajador");

            verify(tareaRepo, never()).guardar(any());
        }

        @Test
        @DisplayName("Administrador puede mover cualquier tarea incluso si está asignada a otro o en backlog")
        void adminMueveTarea_debePermitirCambio() {
            // Arrange: Tarea sin asignar (backlog)
            Tarea tareaBacklog = tareaPorHacer();
            tareaBacklog.setIdTrabajadorAsignado(null);

            when(tareaRepo.buscarPorId(100L)).thenReturn(Optional.of(tareaBacklog));
            when(usuarioRepo.buscarPorEmail("admin@plantopolis.com")).thenReturn(Optional.of(adminDemo()));
            when(tareaRepo.guardar(any(Tarea.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Tarea resultado = sut.cambiarEstadoTarea(100L, "EN_PROGRESO", "Moviendo desde panel admin", "admin@plantopolis.com");

            // Assert
            assertThat(resultado.getEstadoTarea()).isEqualTo("EN_PROGRESO");
            verify(tareaRepo).guardar(any());
        }

        @Test
        @DisplayName("COMPLETADA es terminal: intentar moverla lanza excepción")
        void completadaEsTerminal_debeLanzarExcepcion() {
            // Arrange
            Tarea completada = tareaPorHacer();
            completada.setEstadoTarea(EstadoTarea.COMPLETADA.name());

            when(tareaRepo.buscarPorId(100L)).thenReturn(Optional.of(completada));
            when(usuarioRepo.buscarPorEmail("operario@plantopolis.com")).thenReturn(Optional.of(trabajadorDemo()));

            // Act & Assert
            assertThatThrownBy(() -> sut.cambiarEstadoTarea(100L, "EN_REVISION", null, "operario@plantopolis.com"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Transición no permitida");
        }
    }
}
