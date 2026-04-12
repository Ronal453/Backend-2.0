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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final AuthenticationProvider  authenticationProvider;

    // Todas las rutas de Swagger/OpenAPI sin autenticación
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

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                // ── Swagger sin autenticación ─────────────────────────
                .requestMatchers(SWAGGER_PATHS).permitAll()
                // ── Auth pública ──────────────────────────────────────
                .requestMatchers("/api/auth/**").permitAll()
                // ── Catálogo público ──────────────────────────────────
                .requestMatchers("/api/productos/**").permitAll()
                // ── Requieren login ───────────────────────────────────
                .requestMatchers("/api/carrito/**").authenticated()
                .requestMatchers("/api/pedidos/**").authenticated()
                // ── Solo admin ────────────────────────────────────────
                .requestMatchers("/api/admin/**").hasRole("ADMINISTRADOR")
                // ── Todo lo demás ─────────────────────────────────────
                .anyRequest().authenticated()
            )
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}