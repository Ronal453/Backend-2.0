package com.plantopolis.backend.infrastructure.config;

import com.plantopolis.backend.infrastructure.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración de Spring Security para Plantopolis.
 *
 * Define qué rutas son públicas (sin JWT) y cuáles requieren autenticación.
 * Usa JWT Stateless: no hay sesiones HTTP, cada request trae su propio token.
 *
 * Ruta destino:
 *   Back/src/main/java/com/plantopolis/backend/
 *   infrastructure/config/SecurityConfig.java
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // Filtro que extrae y valida el token JWT en cada request entrante
    private final JwtAuthenticationFilter jwtFilter;

    // Proveedor de autenticación configurado en BeanConfig (DaoAuthenticationProvider)
    private final AuthenticationProvider authenticationProvider;

    /**
     * Rutas de Swagger UI y documentación OpenAPI.
     * Deben ser accesibles sin autenticación para que el equipo
     * pueda consultar y probar la API desde el navegador.
     */
    private static final String[] SWAGGER_PATHS = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/swagger-ui/index.html",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/api-docs",
            "/api-docs/**",
            "/webjars/**"
    };

    /**
     * Configura la cadena de filtros de seguridad HTTP.
     *
     * Orden de evaluación (de arriba hacia abajo, primera regla que coincide gana):
     *   1. Swagger        → permitAll
     *   2. /api/test/**   → permitAll (diagnóstico de email, solo desarrollo)
     *   3. /api/auth/**   → permitAll (login y registro públicos)
     *   4. /api/productos → permitAll (catálogo público)
     *   5. /api/carrito   → authenticated (requiere JWT)
     *   6. /api/pedidos   → authenticated (requiere JWT)
     *   7. /api/admin     → hasRole ADMINISTRADOR
     *   8. anyRequest     → authenticated (por defecto)
     *
     * @param http objeto de configuración HTTP de Spring Security
     * @return cadena de filtros lista para usar
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Deshabilitar CSRF: no aplica en APIs REST con JWT (sin cookies de sesión)
            .csrf(AbstractHttpConfigurer::disable)

            // ── Reglas de autorización por ruta ──────────────────────────────
            .authorizeHttpRequests(auth -> auth

                // Swagger UI y OpenAPI spec → acceso público para documentación
                .requestMatchers(SWAGGER_PATHS).permitAll()

                // Endpoints de diagnóstico/prueba → acceso público (solo desarrollo)
                // IMPORTANTE: eliminar o restringir antes de subir a producción
                .requestMatchers("/api/test/**").permitAll()

                // Autenticación → público (cualquiera puede registrarse o hacer login)
                .requestMatchers("/api/auth/**").permitAll()

                // Catálogo de productos → público (visitantes pueden ver sin login)
                .requestMatchers("/api/productos/**").permitAll()

                // Carrito → requiere JWT válido (solo usuarios autenticados)
                .requestMatchers("/api/carrito/**").authenticated()

                // Pedidos → requiere JWT válido (checkout e historial)
                .requestMatchers("/api/pedidos/**").authenticated()

                // Panel admin → requiere JWT + rol ADMINISTRADOR específicamente
                .requestMatchers("/api/admin/**").hasRole("ADMINISTRADOR")

                // Rutas operativas del trabajador → accesibles por TRABAJADOR y ADMINISTRADOR
                .requestMatchers("/api/tareas/**").hasAnyRole("TRABAJADOR", "ADMINISTRADOR")
                .requestMatchers("/api/lotes/**").hasAnyRole("TRABAJADOR", "ADMINISTRADOR")
                .requestMatchers("/api/mermas/**").hasAnyRole("TRABAJADOR", "ADMINISTRADOR")
                .requestMatchers("/api/zonas/**").hasAnyRole("TRABAJADOR", "ADMINISTRADOR")

                // Cualquier otra ruta no listada → requiere JWT por defecto
                .anyRequest().authenticated()
            )

            // Sin sesiones HTTP: cada request es independiente (stateless)
            // El estado de autenticación se lleva en el Bearer token JWT
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Registrar el proveedor de autenticación con BCrypt y UserDetailsService
            .authenticationProvider(authenticationProvider)

            // Insertar el filtro JWT ANTES del filtro estándar de Spring
            // Así, si el JWT es válido, el contexto de seguridad ya está listo
            // cuando el request llega al controller
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}