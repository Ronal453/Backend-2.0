package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.*;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminTareaService")
class AdminTareaServiceTest {

    @Mock private TareaRepositoryPort tareaRepo;
    @Mock private HistorialTareaRepositoryPort historialRepo;
    @Mock private UsuarioRepositoryPort usuarioRepo;
    @Mock private ValidadorTareas validadorTareas;

    @InjectMocks
    private AdminTareaService sut;

    private Usuario adminDemo() {
        return Usuario.builder()
                .idUsuario(1L)
                .nombreCompleto("Admin General")
                .correo("admin@plantopolis.com")
                .idRol(1L)
                .rolNombre("ADMINISTRADOR")
                .build();
    }

    private Usuario trabajadorDemo() {
        return Usuario.builder()
                .idUsuario(10L)
                .nombreCompleto("Juan Operario")
                .correo("operario@plantopolis.com")
                .idRol(2L)
                .rolNombre("TRABAJADOR")
                .activo(true)
                .build();
    }

    @Nested
    @DisplayName("crearTarea()")
    class CrearTareaTests {

        @Test
        @DisplayName("Crea tarea exitosamente y registra auditoría de CREACION")
        void crearTarea_valida_guardaYRegistraHistorial() {
            // Arrange
            Tarea entrada = Tarea.builder()
                    .tipoTarea("RIEGO")
                    .prioridad("ALTA")
                    .descripcion("Riego por goteo")
                    .idZona(5L)
                    .fechaLimite(LocalDate.now().plusDays(2))
                    .idTrabajadorAsignado(10L)
                    .build();

            when(usuarioRepo.buscarPorEmail("admin@plantopolis.com")).thenReturn(Optional.of(adminDemo()));
            when(tareaRepo.guardar(any(Tarea.class))).thenAnswer(invocation -> {
                Tarea t = invocation.getArgument(0);
                t.setIdTarea(500L);
                return t;
            });

            // Act
            Tarea creada = sut.crearTarea(entrada, "admin@plantopolis.com");

            // Assert
            assertThat(creada.getIdTarea()).isEqualTo(500L);
            assertThat(creada.getEstadoTarea()).isEqualTo("POR_HACER");
            assertThat(creada.getAlertaVencidaEnviada()).isFalse();

            verify(validadorTareas).validarDefinicionTarea("RIEGO", "ALTA", 5L, null, 10L);
            verify(historialRepo).guardar(argThat(h ->
                    "CREACION".equals(h.getTipoCambio()) &&
                    "POR_HACER".equals(h.getEstadoNuevo()) &&
                    Long.valueOf(10L).equals(h.getIdTrabajadorNuevo())
            ));
        }

        @Test
        @DisplayName("Rechaza tarea con fecha límite pasada")
        void crearTarea_fechaPasada_lanzaExcepcion() {
            Tarea entrada = Tarea.builder()
                    .tipoTarea("RIEGO")
                    .prioridad("ALTA")
                    .idZona(5L)
                    .fechaLimite(LocalDate.now().minusDays(1))
                    .build();

            when(usuarioRepo.buscarPorEmail("admin@plantopolis.com")).thenReturn(Optional.of(adminDemo()));

            assertThatThrownBy(() -> sut.crearTarea(entrada, "admin@plantopolis.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no puede ser anterior a la fecha actual");

            verify(tareaRepo, never()).guardar(any());
        }
    }

    @Nested
    @DisplayName("reasignarTarea()")
    class ReasignarTareaTests {

        @Test
        @DisplayName("Reasigna tarea exitosamente a nuevo trabajador y registra auditoría")
        void reasignar_nuevoTrabajador_actualizaYAudita() {
            Tarea existente = Tarea.builder()
                    .idTarea(200L)
                    .estadoTarea("EN_PROGRESO")
                    .idTrabajadorAsignado(10L)
                    .build();

            when(tareaRepo.buscarPorId(200L)).thenReturn(Optional.of(existente));
            when(usuarioRepo.buscarPorEmail("admin@plantopolis.com")).thenReturn(Optional.of(adminDemo()));
            when(tareaRepo.guardar(any(Tarea.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Tarea resultado = sut.reasignarTarea(200L, 20L, "admin@plantopolis.com");

            // Assert
            assertThat(resultado.getIdTrabajadorAsignado()).isEqualTo(20L);
            verify(validadorTareas).validarTrabajadorAsignado(20L);
            verify(historialRepo).guardar(argThat(h ->
                    "REASIGNACION".equals(h.getTipoCambio()) &&
                    Long.valueOf(10L).equals(h.getIdTrabajadorAnterior()) &&
                    Long.valueOf(20L).equals(h.getIdTrabajadorNuevo())
            ));
        }

        @Test
        @DisplayName("Reasignar tarea COMPLETADA lanza IllegalStateException")
        void reasignar_tareaCompletada_lanzaExcepcion() {
            Tarea completada = Tarea.builder()
                    .idTarea(200L)
                    .estadoTarea("COMPLETADA")
                    .idTrabajadorAsignado(10L)
                    .build();

            when(tareaRepo.buscarPorId(200L)).thenReturn(Optional.of(completada));

            assertThatThrownBy(() -> sut.reasignarTarea(200L, 20L, "admin@plantopolis.com"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No es posible reasignar una tarea que ya ha sido completada");

            verify(tareaRepo, never()).guardar(any());
        }
    }

    @Nested
    @DisplayName("obtenerResumen()")
    class ResumenTests {

        @Test
        @DisplayName("Calcula KPIs operativos de forma consistente")
        void obtenerResumen_calculaMetricas() {
            LocalDate hoy = LocalDate.now();

            Tarea t1 = Tarea.builder().idTarea(1L).estadoTarea("POR_HACER").idTrabajadorAsignado(10L).fechaLimite(hoy.plusDays(2)).build();
            Tarea t2 = Tarea.builder().idTarea(2L).estadoTarea("EN_PROGRESO").idTrabajadorAsignado(10L).fechaLimite(hoy.minusDays(1)).build(); // vencida
            Tarea t3 = Tarea.builder().idTarea(3L).estadoTarea("POR_HACER").idTrabajadorAsignado(null).fechaLimite(hoy.plusDays(1)).build(); // backlog
            Tarea t4 = Tarea.builder().idTarea(4L).estadoTarea("COMPLETADA").idTrabajadorAsignado(10L).fechaCompletada(LocalDateTime.now()).fechaLimite(hoy.minusDays(3)).build();

            when(tareaRepo.buscarTodasParaResumen()).thenReturn(List.of(t1, t2, t3, t4));
            when(usuarioRepo.listarActivosPorRol(2L)).thenReturn(List.of(trabajadorDemo()));

            // Act
            ResumenTareas resumen = sut.obtenerResumen(hoy.minusDays(7), hoy);

            // Assert
            assertThat(resumen.getTotalActivas()).isEqualTo(3);
            assertThat(resumen.getVencidasActivas()).isEqualTo(1);
            assertThat(resumen.getSinAsignar()).isEqualTo(1);
            assertThat(resumen.getCompletadasEnPeriodo()).isEqualTo(1);
            assertThat(resumen.getTareasVencidas()).hasSize(1);
            assertThat(resumen.getCargaPorTrabajador()).hasSize(1);
        }
    }
}
