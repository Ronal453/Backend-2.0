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

    // Rutas de Swagger que no requieren autenticación
    private static final String[] SWAGGER_PATHS = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/api-docs",
            "/api-docs/**",
            "/v3/api-docs",
            "/v3/api-docs/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                // ── Swagger — sin autenticación ───────────────────────
                .requestMatchers(SWAGGER_PATHS).permitAll()
                // ── Autenticación pública ─────────────────────────────
                .requestMatchers("/api/auth/**").permitAll()
                // ── Catálogo público ──────────────────────────────────
                .requestMatchers("/api/productos/**").permitAll()
                // ── Carrito y pedidos — requieren login ───────────────
                .requestMatchers("/api/carrito/**").authenticated()
                .requestMatchers("/api/pedidos/**").authenticated()
                // ── Panel admin — solo ADMINISTRADOR ─────────────────
                .requestMatchers("/api/admin/**").hasRole("ADMINISTRADOR")
                // ── Todo lo demás — requiere login ────────────────────
                .anyRequest().authenticated()
            )
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}