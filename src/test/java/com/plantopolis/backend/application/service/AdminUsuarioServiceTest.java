package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.Usuario;
import com.plantopolis.backend.domain.port.out.UsuarioRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUsuarioService")
class AdminUsuarioServiceTest {

    @Mock private UsuarioRepositoryPort usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminUsuarioService sut;

    @Nested
    @DisplayName("crearTrabajador()")
    class CrearTrabajadorTests {

        @Test
        @DisplayName("Debería crear el trabajador con rol TRABAJADOR (id=2) y activo=true")
        void crearTrabajador_datosValidos_creaConRolTrabajador() {
            when(usuarioRepository.existePorEmail("carlos@plantopolis.com")).thenReturn(false);
            when(passwordEncoder.encode("Temporal2024")).thenReturn("$2a$hashed");
            when(usuarioRepository.guardar(any(Usuario.class)))
                    .thenAnswer(i -> i.getArgument(0));

            Usuario resultado = sut.crearTrabajador(
                    "Carlos Gómez", "carlos@plantopolis.com", "Temporal2024");

            assertThat(resultado.getIdRol()).isEqualTo(2L);
            assertThat(resultado.getActivo()).isTrue();
            assertThat(resultado.getNombreCompleto()).isEqualTo("Carlos Gómez");
            assertThat(resultado.getCorreo()).isEqualTo("carlos@plantopolis.com");
        }

        @Test
        @DisplayName("Debería hashear la contraseña inicial antes de guardar")
        void crearTrabajador_debeHashearPassword() {
            when(usuarioRepository.existePorEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode("miPassword")).thenReturn("$2a$hash_seguro");
            when(usuarioRepository.guardar(any(Usuario.class)))
                    .thenAnswer(i -> i.getArgument(0));

            sut.crearTrabajador("Ana", "ana@plantopolis.com", "miPassword");

            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).guardar(captor.capture());
            assertThat(captor.getValue().getContrasenaHash()).isEqualTo("$2a$hash_seguro");
        }

        @Test
        @DisplayName("Debería lanzar excepción cuando el correo ya está registrado")
        void crearTrabajador_correoDuplicado_lanzaExcepcion() {
            when(usuarioRepository.existePorEmail("existe@plantopolis.com")).thenReturn(true);

            assertThatThrownBy(() ->
                    sut.crearTrabajador("Alguien", "existe@plantopolis.com", "pass123"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("ya está registrado");

            verify(usuarioRepository, never()).guardar(any());
        }

        @Test
        @DisplayName("Debería asignar fechaRegistro y fechaActualizacion no nulas")
        void crearTrabajador_asignaFechas() {
            when(usuarioRepository.existePorEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hash");
            when(usuarioRepository.guardar(any(Usuario.class)))
                    .thenAnswer(i -> i.getArgument(0));

            Usuario resultado = sut.crearTrabajador("Luis", "luis@plantopolis.com", "pass123");

            assertThat(resultado.getFechaRegistro()).isNotNull();
            assertThat(resultado.getFechaActualizacion()).isNotNull();
        }
    }
}