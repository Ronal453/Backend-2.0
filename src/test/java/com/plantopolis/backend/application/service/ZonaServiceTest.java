package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.Zona;
import com.plantopolis.backend.domain.port.out.LoteRepositoryPort;
import com.plantopolis.backend.domain.port.out.ZonaRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZonaServiceTest {

    @Mock
    private ZonaRepositoryPort zonaRepository;

    @Mock
    private LoteRepositoryPort loteRepository;

    @InjectMocks
    private ZonaService zonaService;

    private Zona zonaActiva;

    @BeforeEach
    void setUp() {
        zonaActiva = Zona.builder()
                .idZona(1L)
                .nombre("Invernadero A")
                .capacidadMaxima(20)
                .tipoCondicion("INVERNADERO")
                .exposicionSolar("PLENO_SOL")
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("listarZonasActivas devuelve solo zonas activas")
    void listarZonasActivas_exitoso() {
        when(zonaRepository.listarActivas()).thenReturn(List.of(zonaActiva));

        List<Zona> resultado = zonaService.listarZonasActivas();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Invernadero A");
        verify(zonaRepository).listarActivas();
    }

    @Test
    @DisplayName("listarTodasLasZonas devuelve activas e inactivas")
    void listarTodasLasZonas_exitoso() {
        Zona zonaInactiva = Zona.builder().idZona(2L).nombre("Zona B").activo(false).build();
        when(zonaRepository.listarTodas()).thenReturn(List.of(zonaActiva, zonaInactiva));

        List<Zona> resultado = zonaService.listarTodasLasZonas();

        assertThat(resultado).hasSize(2);
        verify(zonaRepository).listarTodas();
    }

    @Test
    @DisplayName("crearZona exitoso cuando el nombre no existe")
    void crearZona_exitoso() {
        Zona nueva = Zona.builder()
                .nombre("Invernadero C")
                .capacidadMaxima(15)
                .tipoCondicion("INVERNADERO")
                .exposicionSolar("MEDIA_SOMBRA")
                .build();

        when(zonaRepository.existePorNombre("Invernadero C")).thenReturn(false);
        when(zonaRepository.guardar(any(Zona.class))).thenAnswer(inv -> inv.getArgument(0));

        Zona creada = zonaService.crearZona(nueva);

        assertThat(creada.getActivo()).isTrue();
        assertThat(creada.getFechaCreacion()).isNotNull();
        verify(zonaRepository).guardar(nueva);
    }

    @Test
    @DisplayName("crearZona lanza excepcion si el nombre ya esta registrado")
    void crearZona_nombreDuplicado_lanzaExcepcion() {
        when(zonaRepository.existePorNombre("Invernadero A")).thenReturn(true);

        assertThatThrownBy(() -> zonaService.crearZona(zonaActiva))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe una zona con el nombre");

        verify(zonaRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("actualizarZona exitoso si existe y el nombre no colisiona")
    void actualizarZona_exitoso() {
        Zona datosActualizar = Zona.builder()
                .nombre("Invernadero A Modificado")
                .capacidadMaxima(30)
                .tipoCondicion("INVERNADERO")
                .exposicionSolar("PLENO_SOL")
                .build();

        when(zonaRepository.buscarPorId(1L)).thenReturn(Optional.of(zonaActiva));
        when(zonaRepository.existePorNombreYDistintoId("Invernadero A Modificado", 1L)).thenReturn(false);
        when(loteRepository.contarLotesActivosPorZona(1L)).thenReturn(5L);
        when(zonaRepository.guardar(any(Zona.class))).thenAnswer(inv -> inv.getArgument(0));

        Zona actualizada = zonaService.actualizarZona(1L, datosActualizar, false);

        assertThat(actualizada.getNombre()).isEqualTo("Invernadero A Modificado");
        assertThat(actualizada.getCapacidadMaxima()).isEqualTo(30);
        verify(zonaRepository).guardar(zonaActiva);
    }

    @Test
    @DisplayName("actualizarZona lanza advertencia si reduce capacidad por debajo de ocupacion sin confirmacion (HU21b)")
    void actualizarZona_reduceCapacidadSinConfirmacion_lanzaAdvertencia() {
        Zona datosReducir = Zona.builder()
                .nombre("Invernadero A")
                .capacidadMaxima(3) // menor a 5 lotes activos
                .tipoCondicion("INVERNADERO")
                .exposicionSolar("PLENO_SOL")
                .build();

        when(zonaRepository.buscarPorId(1L)).thenReturn(Optional.of(zonaActiva));
        when(zonaRepository.existePorNombreYDistintoId("Invernadero A", 1L)).thenReturn(false);
        when(loteRepository.contarLotesActivosPorZona(1L)).thenReturn(5L);

        assertThatThrownBy(() -> zonaService.actualizarZona(1L, datosReducir, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ADVERTENCIA_CAPACIDAD");

        verify(zonaRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("actualizarZona permite reducir capacidad por debajo de ocupacion con confirmacion explicita (HU21b)")
    void actualizarZona_reduceCapacidadConConfirmacion_exitoso() {
        Zona datosReducir = Zona.builder()
                .nombre("Invernadero A")
                .capacidadMaxima(3)
                .tipoCondicion("INVERNADERO")
                .exposicionSolar("PLENO_SOL")
                .build();

        when(zonaRepository.buscarPorId(1L)).thenReturn(Optional.of(zonaActiva));
        when(zonaRepository.existePorNombreYDistintoId("Invernadero A", 1L)).thenReturn(false);
        when(loteRepository.contarLotesActivosPorZona(1L)).thenReturn(5L);
        when(zonaRepository.guardar(any(Zona.class))).thenAnswer(inv -> inv.getArgument(0));

        Zona actualizada = zonaService.actualizarZona(1L, datosReducir, true);

        assertThat(actualizada.getCapacidadMaxima()).isEqualTo(3);
        verify(zonaRepository).guardar(zonaActiva);
    }

    @Test
    @DisplayName("actualizarZona lanza excepcion si la zona no existe")
    void actualizarZona_noExiste_lanzaExcepcion() {
        when(zonaRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> zonaService.actualizarZona(99L, zonaActiva, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Zona no encontrada");

        verify(zonaRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("activarZona pone activo en true")
    void activarZona_exitoso() {
        zonaActiva.setActivo(false);
        when(zonaRepository.buscarPorId(1L)).thenReturn(Optional.of(zonaActiva));
        when(zonaRepository.guardar(any(Zona.class))).thenAnswer(inv -> inv.getArgument(0));

        Zona activada = zonaService.activarZona(1L);

        assertThat(activada.getActivo()).isTrue();
        verify(zonaRepository).guardar(zonaActiva);
    }

    @Test
    @DisplayName("desactivarZona exitoso cuando no tiene lotes activos")
    void desactivarZona_sinLotesActivos_exitoso() {
        when(zonaRepository.buscarPorId(1L)).thenReturn(Optional.of(zonaActiva));
        when(loteRepository.contarLotesActivosPorZona(1L)).thenReturn(0L);
        when(zonaRepository.guardar(any(Zona.class))).thenAnswer(inv -> inv.getArgument(0));

        Zona desactivada = zonaService.desactivarZona(1L);

        assertThat(desactivada.getActivo()).isFalse();
        verify(zonaRepository).guardar(zonaActiva);
    }

    @Test
    @DisplayName("desactivarZona lanza IllegalStateException si tiene lotes activos")
    void desactivarZona_conLotesActivos_lanzaExcepcion() {
        when(zonaRepository.buscarPorId(1L)).thenReturn(Optional.of(zonaActiva));
        when(loteRepository.contarLotesActivosPorZona(1L)).thenReturn(3L);

        assertThatThrownBy(() -> zonaService.desactivarZona(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede desactivar la zona porque tiene 3 lotes activos");

        verify(zonaRepository, never()).guardar(any());
    }
}
