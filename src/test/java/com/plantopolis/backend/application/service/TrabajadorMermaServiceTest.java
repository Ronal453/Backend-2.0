package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.CausaMerma;
import com.plantopolis.backend.domain.model.LoteProduccion;
import com.plantopolis.backend.domain.model.Merma;
import com.plantopolis.backend.domain.model.Usuario;
import com.plantopolis.backend.domain.port.out.CausaMermaRepositoryPort;
import com.plantopolis.backend.domain.port.out.HistorialEstadoLoteRepositoryPort;
import com.plantopolis.backend.domain.port.out.LoteRepositoryPort;
import com.plantopolis.backend.domain.port.out.MermaRepositoryPort;
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
@DisplayName("TrabajadorMermaService")
class TrabajadorMermaServiceTest {

    @Mock private MermaRepositoryPort mermaRepo;
    @Mock private CausaMermaRepositoryPort causaRepo;
    @Mock private LoteRepositoryPort loteRepo;
    @Mock private HistorialEstadoLoteRepositoryPort historialLoteRepo;
    @Mock private UsuarioRepositoryPort usuarioRepo;

    @InjectMocks
    private TrabajadorMermaService sut;

    private Usuario trabajadorDemo() {
        return Usuario.builder()
                .idUsuario(10L)
                .nombreCompleto("Juan Operario")
                .correo("operario@plantopolis.com")
                .build();
    }

    private LoteProduccion loteDemo(int stock) {
        return LoteProduccion.builder()
                .idLote(50L)
                .codigoLote("LOT-2024-001")
                .especie("Monstera Deliciosa")
                .cantidadInicial(100)
                .cantidadActual(stock)
                .estadoLote("CRECIENDO")
                .build();
    }

    private CausaMerma causaDemo() {
        return CausaMerma.builder()
                .idCausa(1L)
                .nombreCausa("PLAGA")
                .activo(true)
                .build();
    }

    @Nested
    @DisplayName("registrarMerma()")
    class RegistrarMermaTests {

        @Test
        @DisplayName("Descuenta stock del lote correctamente y guarda la merma")
        void registrarMerma_valida_descuentaCantidadYGuarda() {
            // Arrange
            var lote = loteDemo(50);
            when(loteRepo.buscarPorId(50L)).thenReturn(Optional.of(lote));
            when(causaRepo.buscarPorId(1L)).thenReturn(Optional.of(causaDemo()));
            when(usuarioRepo.buscarPorEmail("operario@plantopolis.com")).thenReturn(Optional.of(trabajadorDemo()));
            when(mermaRepo.guardar(any(Merma.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            Merma resultado = sut.registrarMerma(50L, 1L, 10, LocalDate.now(), "Ataque de pulgón", "operario@plantopolis.com");

            // Assert
            assertThat(resultado.getCantidadPerdida()).isEqualTo(10);
            assertThat(lote.getCantidadActual()).isEqualTo(40);
            verify(loteRepo).guardar(lote);
            verify(mermaRepo).guardar(any(Merma.class));
        }

        @Test
        @DisplayName("Lanza excepción si la cantidad de merma supera la cantidad actual del lote")
        void registrarMerma_cantidadSuperaStock_lanzaExcepcion() {
            // Arrange
            var lote = loteDemo(20);
            when(loteRepo.buscarPorId(50L)).thenReturn(Optional.of(lote));
            when(causaRepo.buscarPorId(1L)).thenReturn(Optional.of(causaDemo()));
            when(usuarioRepo.buscarPorEmail("operario@plantopolis.com")).thenReturn(Optional.of(trabajadorDemo()));

            // Act & Assert
            assertThatThrownBy(() -> sut.registrarMerma(50L, 1L, 25, LocalDate.now(), "Daño excesivo", "operario@plantopolis.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("supera la cantidad actual disponible");

            verify(mermaRepo, never()).guardar(any());
        }
    }
}
