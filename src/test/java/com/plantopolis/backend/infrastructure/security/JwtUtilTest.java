package com.plantopolis.backend.infrastructure.security;

import com.plantopolis.backend.infrastructure.config.JwtConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para JwtUtil.
 *
 * Cubre:
 *   - generarToken(): token no nulo, contiene email y rol
 *   - extraerEmail(): del token generado
 *   - extraerRol(): CLIENTE y ADMINISTRADOR
 *   - esValido(): token válido, expirado (simulado), usuario diferente
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtUtil")
class JwtUtilTest {

    @Mock private JwtConfig jwtConfig;

    @InjectMocks
    private JwtUtil sut;

    // Clave secreta mínima para HS256 (≥ 256 bits = 32 chars)
    private static final String SECRET =
            "plantopolisJwtSecretKey2024MuyLargaMinimo256BitsParaHMACSHA";
    private static final long EXPIRACION_24H = 86_400_000L;
    private static final long EXPIRACION_1MS  = 1L; // para simular expirado

    @BeforeEach
    void setUp() {
        when(jwtConfig.getSecret()).thenReturn(SECRET);
        lenient().when(jwtConfig.getExpiration()).thenReturn(EXPIRACION_24H);
    }

    // ── Fixture ───────────────────────────────────────────────────────────────

    private UserDetails userDetails(String email, String rol) {
        return User.builder()
                .username(email)
                .password("hash")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + rol)))
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GENERAR TOKEN
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("generarToken()")
    class GenerarTokenTests {

        @Test
        @DisplayName("Debería retornar un token no nulo ni vacío")
        void generarToken_retornaTokenNoVacio() {
            // Arrange
            UserDetails user = userDetails("ronal@test.com", "CLIENTE");

            // Act
            String token = sut.generarToken(user, "CLIENTE");

            // Assert
            assertThat(token).isNotBlank();
        }

        @Test
        @DisplayName("Debería generar un JWT con tres partes separadas por puntos")
        void generarToken_tieneTresParts() {
            // Arrange
            UserDetails user = userDetails("ronal@test.com", "CLIENTE");

            // Act
            String token = sut.generarToken(user, "CLIENTE");

            // Assert
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("Debería generar tokens distintos para usuarios distintos")
        void generarToken_usuariosDiferentes_tokensDistintos() {
            // Arrange
            UserDetails cliente = userDetails("cliente@test.com", "CLIENTE");
            UserDetails admin   = userDetails("admin@test.com",   "ADMINISTRADOR");

            // Act
            String tokenCliente = sut.generarToken(cliente, "CLIENTE");
            String tokenAdmin   = sut.generarToken(admin,   "ADMINISTRADOR");

            // Assert
            assertThat(tokenCliente).isNotEqualTo(tokenAdmin);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // EXTRAER EMAIL
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("extraerEmail()")
    class ExtraerEmailTests {

        @Test
        @DisplayName("Debería extraer el email correcto del token")
        void extraerEmail_tokenValido_retornaEmail() {
            // Arrange
            UserDetails user = userDetails("ronal@test.com", "CLIENTE");
            String token = sut.generarToken(user, "CLIENTE");

            // Act
            String email = sut.extraerEmail(token);

            // Assert
            assertThat(email).isEqualTo("ronal@test.com");
        }

        @Test
        @DisplayName("Debería extraer el email correcto para un administrador")
        void extraerEmail_tokenAdmin_retornaEmailAdmin() {
            // Arrange
            UserDetails admin = userDetails("admin@plantopolis.com", "ADMINISTRADOR");
            String token = sut.generarToken(admin, "ADMINISTRADOR");

            // Act
            String email = sut.extraerEmail(token);

            // Assert
            assertThat(email).isEqualTo("admin@plantopolis.com");
        }

        @Test
        @DisplayName("Debería extraer emails con caracteres especiales")
        void extraerEmail_emailConPunto_retornaCorrectamente() {
            // Arrange
            UserDetails user = userDetails("primer.apellido@empresa.co", "CLIENTE");
            String token = sut.generarToken(user, "CLIENTE");

            // Act
            String email = sut.extraerEmail(token);

            // Assert
            assertThat(email).isEqualTo("primer.apellido@empresa.co");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // EXTRAER ROL
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("extraerRol()")
    class ExtraerRolTests {

        @Test
        @DisplayName("Debería extraer rol CLIENTE del token")
        void extraerRol_rolCliente_retornaCliente() {
            // Arrange
            String token = sut.generarToken(userDetails("u@t.com", "CLIENTE"), "CLIENTE");

            // Act
            String rol = sut.extraerRol(token);

            // Assert
            assertThat(rol).isEqualTo("CLIENTE");
        }

        @Test
        @DisplayName("Debería extraer rol ADMINISTRADOR del token")
        void extraerRol_rolAdministrador_retornaAdministrador() {
            // Arrange
            String token = sut.generarToken(
                    userDetails("admin@t.com", "ADMINISTRADOR"), "ADMINISTRADOR");

            // Act
            String rol = sut.extraerRol(token);

            // Assert
            assertThat(rol).isEqualTo("ADMINISTRADOR");
        }

        @Test
        @DisplayName("Debería diferenciar el rol entre cliente y admin en tokens distintos")
        void extraerRol_tokensDiferentes_rolesCorrectos() {
            // Arrange
            String tokenCliente = sut.generarToken(
                    userDetails("c@t.com", "CLIENTE"), "CLIENTE");
            String tokenAdmin = sut.generarToken(
                    userDetails("a@t.com", "ADMINISTRADOR"), "ADMINISTRADOR");

            // Act & Assert
            assertThat(sut.extraerRol(tokenCliente)).isEqualTo("CLIENTE");
            assertThat(sut.extraerRol(tokenAdmin)).isEqualTo("ADMINISTRADOR");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ES VÁLIDO
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("esValido()")
    class EsValidoTests {

        @Test
        @DisplayName("Debería retornar true para un token válido del mismo usuario")
        void esValido_tokenValidoMismoUsuario_retornaTrue() {
            // Arrange
            UserDetails user = userDetails("ronal@test.com", "CLIENTE");
            String token = sut.generarToken(user, "CLIENTE");

            // Act
            boolean valido = sut.esValido(token, user);

            // Assert
            assertThat(valido).isTrue();
        }

        @Test
        @DisplayName("Debería retornar false cuando el token pertenece a otro usuario")
        void esValido_tokenDeOtroUsuario_retornaFalse() {
            // Arrange
            UserDetails ronal = userDetails("ronal@test.com",  "CLIENTE");
            UserDetails nicole = userDetails("nicole@test.com", "CLIENTE");
            String tokenRonal = sut.generarToken(ronal, "CLIENTE");

            // Act — token de Ronal validado contra Nicole
            boolean valido = sut.esValido(tokenRonal, nicole);

            // Assert
            assertThat(valido).isFalse();
        }

        @Test
        @DisplayName("Debería retornar false para un token expirado")
        void esValido_tokenExpirado_retornaFalse() throws InterruptedException {
            // Arrange — configurar expiración de 1 ms
            when(jwtConfig.getExpiration()).thenReturn(EXPIRACION_1MS);

            UserDetails user = userDetails("ronal@test.com", "CLIENTE");
            String token = sut.generarToken(user, "CLIENTE");

            // Esperar a que expire
            Thread.sleep(10);

            // Act
            boolean valido = sut.esValido(token, user);

            // Assert
            assertThat(valido).isFalse();
        }

        @Test
        @DisplayName("Debería retornar false para un token manipulado")
        void esValido_tokenManipulado_retornaFalse() {
            // Arrange
            UserDetails user = userDetails("ronal@test.com", "CLIENTE");
            String tokenOriginal = sut.generarToken(user, "CLIENTE");

            // Manipular la firma (tercera parte del JWT)
            String[] partes = tokenOriginal.split("\\.");
            String tokenManipulado = partes[0] + "." + partes[1] + ".firmaFalsa";

            // Act
            boolean valido = sut.esValido(tokenManipulado, user);

            // Assert
            assertThat(valido).isFalse();
        }

        @Test
        @DisplayName("Debería retornar false para un token completamente inválido")
        void esValido_tokenInvalido_retornaFalse() {
            // Arrange
            UserDetails user = userDetails("ronal@test.com", "CLIENTE");

            // Act
            boolean valido = sut.esValido("esto.no.esUnJWT", user);

            // Assert
            assertThat(valido).isFalse();
        }

        @Test
        @DisplayName("Debería retornar false para un token vacío")
        void esValido_tokenVacio_retornaFalse() {
            // Arrange
            UserDetails user = userDetails("ronal@test.com", "CLIENTE");

            // Act
            boolean valido = sut.esValido("", user);

            // Assert
            assertThat(valido).isFalse();
        }
    }
}
