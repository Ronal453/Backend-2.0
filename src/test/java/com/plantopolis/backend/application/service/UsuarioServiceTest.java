package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.Usuario;
import com.plantopolis.backend.domain.port.out.UsuarioRepositoryPort;
import com.plantopolis.backend.infrastructure.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para UsuarioService.
 *
 * Cubre:
 *   - registrar(): happy path, email duplicado
 *   - login(): happy path, credenciales incorrectas, rol null
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService")
class UsuarioServiceTest {

    @Mock private UsuarioRepositoryPort usuarioRepository;
    @Mock private PasswordEncoder       passwordEncoder;
    @Mock private AuthenticationManager authManager;
    @Mock private JwtUtil               jwtUtil;
    @Mock private UserDetailsService    userDetailsService;

    @InjectMocks
    private UsuarioService sut; // System Under Test

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private Usuario usuarioGuardado() {
        return Usuario.builder()
                .idUsuario(1L)
                .nombreCompleto("Ronal Alarcón")
                .correo("ronal@test.com")
                .contrasenaHash("$2a$hashed")
                .idRol(3L)
                .rolNombre("CLIENTE")
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // REGISTRAR
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("registrar()")
    class RegistrarTests {

        @Test
        @DisplayName("Debería registrar usuario cuando el email no existe")
        void registrar_emailNuevo_retornaUsuario() {
            // Arrange
            when(usuarioRepository.existePorEmail("ronal@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$hashed");
            when(usuarioRepository.guardar(any(Usuario.class))).thenReturn(usuarioGuardado());

            // Act
            Usuario resultado = sut.registrar(
                    "Ronal Alarcón", "ronal@test.com",
                    "password123", "3001234567", "Calle 123");

            // Assert
            assertThat(resultado).isNotNull();
            assertThat(resultado.getCorreo()).isEqualTo("ronal@test.com");
            assertThat(resultado.getNombreCompleto()).isEqualTo("Ronal Alarcón");
        }

        @Test
        @DisplayName("Debería hashear la contraseña antes de guardar")
        void registrar_debeHashearPassword() {
            // Arrange
            when(usuarioRepository.existePorEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode("mi_password")).thenReturn("$2a$hash_seguro");
            when(usuarioRepository.guardar(any(Usuario.class))).thenReturn(usuarioGuardado());

            // Act
            sut.registrar("Ana", "ana@test.com", "mi_password", null, null);

            // Assert
            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).guardar(captor.capture());
            assertThat(captor.getValue().getContrasenaHash()).isEqualTo("$2a$hash_seguro");
        }

       @Test
@DisplayName("Debería asignar rol CLIENTE (id=3) al registrar")
void registrar_debeAsignarRolCliente() {
    // Arrange
    when(usuarioRepository.existePorEmail(anyString())).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("hash");
    when(usuarioRepository.guardar(any(Usuario.class))).thenReturn(usuarioGuardado());

    // Act
    sut.registrar("Test", "test@test.com", "pass123", null, null);

    // Assert
    // CAMBIO SCHEMA V2: el orden de roles cambió — antes 1=CLIENTE,
    // ahora 1=ADMINISTRADOR, 2=TRABAJADOR, 3=CLIENTE.
    ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
    verify(usuarioRepository).guardar(captor.capture());
    assertThat(captor.getValue().getIdRol()).isEqualTo(3L);
}

        @Test
        @DisplayName("Debería lanzar excepción cuando el email ya existe")
        void registrar_emailDuplicado_lanzaExcepcion() {
            // Arrange
            when(usuarioRepository.existePorEmail("ronal@test.com")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() ->
                sut.registrar("Ronal", "ronal@test.com", "pass", null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("El email ya está registrado");

            // Verificar que NO se llamó a guardar
            verify(usuarioRepository, never()).guardar(any());
        }

        @Test
        @DisplayName("Debería guardar teléfono y dirección cuando se proporcionan")
        void registrar_conDatosOpcionales_losGuarda() {
            // Arrange
            when(usuarioRepository.existePorEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hash");
            when(usuarioRepository.guardar(any(Usuario.class))).thenReturn(usuarioGuardado());

            // Act
            sut.registrar("Test", "t@t.com", "pass", "3001234567", "Calle 456");

            // Assert
            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).guardar(captor.capture());
            assertThat(captor.getValue().getTelefono()).isEqualTo("3001234567");
            assertThat(captor.getValue().getDireccion()).isEqualTo("Calle 456");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LOGIN
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("login()")
    class LoginTests {

        private UserDetails userDetails() {
            return User.builder()
                    .username("ronal@test.com")
                    .password("$2a$hashed")
                    .roles("CLIENTE")
                    .build();
        }

        @Test
        @DisplayName("Debería retornar token JWT cuando las credenciales son válidas")
        void login_credencialesValidas_retornaToken() {
            // Arrange
            when(userDetailsService.loadUserByUsername("ronal@test.com"))
                    .thenReturn(userDetails());
            when(usuarioRepository.buscarPorEmail("ronal@test.com"))
                    .thenReturn(Optional.of(usuarioGuardado()));
            when(jwtUtil.generarToken(any(UserDetails.class), eq("CLIENTE")))
                    .thenReturn("eyJhbGciOiJIUzI1NiJ9.token");

            // Act
            String token = sut.login("ronal@test.com", "password123");

            // Assert
            assertThat(token).isNotBlank();
            assertThat(token).startsWith("eyJ");
        }

        @Test
        @DisplayName("Debería incluir el rol correcto en el token")
        void login_debeIncluirRolEnToken() {
            // Arrange
            when(userDetailsService.loadUserByUsername(anyString()))
                    .thenReturn(userDetails());
            when(usuarioRepository.buscarPorEmail(anyString()))
                    .thenReturn(Optional.of(usuarioGuardado()));
            when(jwtUtil.generarToken(any(), eq("CLIENTE")))
                    .thenReturn("token_cliente");

            // Act
            sut.login("ronal@test.com", "pass");

            // Assert — verifica que se llama con el rol correcto
            verify(jwtUtil).generarToken(any(UserDetails.class), eq("CLIENTE"));
        }

        @Test
        @DisplayName("Debería usar rol CLIENTE como fallback cuando rolNombre es null")
        void login_rolNull_usaClienteFallback() {
            // Arrange
            Usuario sinRol = Usuario.builder()
                    .idUsuario(2L).correo("sin@rol.com")
                    .contrasenaHash("hash")
                    .rolNombre(null) // ← sin rol asignado
                    .build();

            when(userDetailsService.loadUserByUsername(anyString()))
                    .thenReturn(userDetails());
            when(usuarioRepository.buscarPorEmail(anyString()))
                    .thenReturn(Optional.of(sinRol));
            when(jwtUtil.generarToken(any(), eq("CLIENTE")))
                    .thenReturn("token_fallback");

            // Act & Assert — no debe lanzar excepción
            assertThatCode(() -> sut.login("sin@rol.com", "pass"))
                    .doesNotThrowAnyException();

            verify(jwtUtil).generarToken(any(), eq("CLIENTE"));
        }

        @Test
        @DisplayName("Debería propagar excepción cuando las credenciales son incorrectas")
        void login_credencialesInvalidas_propagaExcepcion() {
            // Arrange
            doThrow(new BadCredentialsException("Credenciales incorrectas"))
                    .when(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

            // Act & Assert
            assertThatThrownBy(() -> sut.login("ronal@test.com", "wrongpass"))
                    .isInstanceOf(BadCredentialsException.class);

            // Nunca debe llegar al JWT
            verify(jwtUtil, never()).generarToken(any(), any());
        }

        @Test
        @DisplayName("Debería autenticar con el email y password correctos")
        void login_llamaAuthManagerConCredenciales() {
            // Arrange
            when(userDetailsService.loadUserByUsername("ronal@test.com"))
                    .thenReturn(userDetails());
            when(usuarioRepository.buscarPorEmail("ronal@test.com"))
                    .thenReturn(Optional.of(usuarioGuardado()));
            when(jwtUtil.generarToken(any(), anyString())).thenReturn("token");

            // Act
            sut.login("ronal@test.com", "password123");

            // Assert
            ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                    ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
            verify(authManager).authenticate(captor.capture());
            assertThat(captor.getValue().getPrincipal()).isEqualTo("ronal@test.com");
            assertThat(captor.getValue().getCredentials()).isEqualTo("password123");
        }
    }
}
