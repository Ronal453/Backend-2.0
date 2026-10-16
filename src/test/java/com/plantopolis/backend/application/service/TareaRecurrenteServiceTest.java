package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.LoteProduccion;
import com.plantopolis.backend.domain.model.Tarea;
import com.plantopolis.backend.domain.model.TareaRecurrente;
import com.plantopolis.backend.domain.model.Usuario;
import com.plantopolis.backend.domain.port.out.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TareaRecurrenteService")
class TareaRecurrenteServiceTest {

    @Mock private TareaRecurrenteRepositoryPort recurrenteRepo;
    @Mock private TareaRepositoryPort tareaRepo;
    @Mock private HistorialTareaRepositoryPort historialRepo;
    @Mock private LoteRepositoryPort loteRepo;
    @Mock private ZonaRepositoryPort zonaRepo;
    @Mock private UsuarioRepositoryPort usuarioRepo;
    @Mock private ValidadorTareas validadorTareas;

    @InjectMocks
    private TareaRecurrenteService sut;

    private Usuario adminDemo() {
        return Usuario.builder().idUsuario(1L).correo("admin@plantopolis.com").build();
    }

    @Test
    @DisplayName("Crear regla para hoy genera inmediatamente la primera tarea y avanza ciclo")
    void crearRegla_paraHoy_generaInstanciaYSalto() {
        LocalDate hoy = LocalDate.now();
        TareaRecurrente def = TareaRecurrente.builder()
                .tipoTarea("RIEGO")
                .prioridad("ALTA")
                .frecuenciaDias(3)
                .idZona(1L)
                .build();

        when(usuarioRepo.buscarPorEmail("admin@plantopolis.com")).thenReturn(Optional.of(adminDemo()));
        when(recurrenteRepo.guardarRecurrente(any(TareaRecurrente.class))).thenAnswer(i -> {
            TareaRecurrente r = i.getArgument(0);
            if (r.getIdTareaRecurrente() == null) r.setIdTareaRecurrente(10L);
            return r;
        });
        when(tareaRepo.guardar(any(Tarea.class))).thenAnswer(i -> {
            Tarea t = i.getArgument(0);
            t.setIdTarea(101L);
            return t;
        });

        // Act
        TareaRecurrente resultado = sut.crear(def, hoy, "admin@plantopolis.com");

        // Assert
        assertThat(resultado.getIdTareaRecurrente()).isEqualTo(10L);
        assertThat(resultado.getFechaProximaEjecucion()).isEqualTo(hoy.plusDays(3));
        verify(tareaRepo).guardar(argThat(t -> Long.valueOf(10L).equals(t.getIdTareaRecurrente()) && hoy.equals(t.getFechaLimite())));
    }

    @Test
    @DisplayName("Frecuencia menor a 1 lanza IllegalArgumentException")
    void crearRegla_frecuenciaInvalida_lanzaExcepcion() {
        TareaRecurrente def = TareaRecurrente.builder()
                .tipoTarea("RIEGO")
                .frecuenciaDias(0)
                .idZona(1L)
                .build();

        when(usuarioRepo.buscarPorEmail("admin@plantopolis.com")).thenReturn(Optional.of(adminDemo()));

        assertThatThrownBy(() -> sut.crear(def, LocalDate.now(), "admin@plantopolis.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("frecuencia en días");
    }

    @Test
    @DisplayName("generarInstanciasPendientes respeta idempotencia y no duplica tarea si ya existe")
    void generarInstancias_idempotencia_noDuplica() {
        LocalDate hoy = LocalDate.now();
        TareaRecurrente regla = TareaRecurrente.builder()
                .idTareaRecurrente(10L)
                .tipoTarea("RIEGO")
                .frecuenciaDias(3)
                .fechaProximaEjecucion(hoy)
                .idZona(1L)
                .activo(true)
                .build();

        when(recurrenteRepo.buscarRecurrentesActivasPendientes(hoy)).thenReturn(List.of(regla));
        when(tareaRepo.existeInstanciaRecurrente(10L, hoy)).thenReturn(true);

        // Act
        int generadas = sut.generarInstanciasPendientes(hoy);

        // Assert
        assertThat(generadas).isEqualTo(0);
        verify(tareaRepo, never()).guardar(any());
        verify(recurrenteRepo).guardarRecurrente(argThat(r -> r.getFechaProximaEjecucion().isAfter(hoy)));
    }

    @Test
    @DisplayName("Desactiva automáticamente la tarea recurrente si el lote asociado está descartado")
    void generarInstancias_loteDescartado_desactivaRegla() {
        LocalDate hoy = LocalDate.now();
        TareaRecurrente regla = TareaRecurrente.builder()
                .idTareaRecurrente(20L)
                .tipoTarea("PODA")
                .frecuenciaDias(5)
                .fechaProximaEjecucion(hoy)
                .idLote(99L)
                .activo(true)
                .build();

        when(recurrenteRepo.buscarRecurrentesActivasPendientes(hoy)).thenReturn(List.of(regla));
        when(tareaRepo.existeInstanciaRecurrente(20L, hoy)).thenReturn(false);
        when(loteRepo.buscarPorId(99L)).thenReturn(Optional.of(LoteProduccion.builder().idLote(99L).estadoLote("DESCARTADO").build()));

        // Act
        int generadas = sut.generarInstanciasPendientes(hoy);

        // Assert
        assertThat(generadas).isEqualTo(0);
        verify(recurrenteRepo).guardarRecurrente(argThat(r -> Boolean.FALSE.equals(r.getActivo())));
        verify(tareaRepo, never()).guardar(any());
    }
}
