package com.plantopolis.backend.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI plantopolisOpenAPI() {
        return new OpenAPI()
                // ── Información general de la API ────────────────────
                .info(new Info()
                        .title("🌱 Plantopolis API")
                        .description("""
                            API REST para la tienda de plantas Plantopolis.
                            
                            ## Funcionalidades
                            - **Autenticación** JWT con roles CLIENTE y ADMINISTRADOR
                            - **Catálogo** de productos con filtros y paginación
                            - **Carrito** de compras con gestión de ítems
                            - **Pedidos** con proceso de pago y notificación por email
                            
                            ## Cómo usar
                            1. Regístrate en `POST /api/auth/registro`
                            2. Inicia sesión en `POST /api/auth/login`
                            3. Copia el token JWT de la respuesta
                            4. Haz clic en **Authorize** e ingresa: `Bearer {token}`
                            5. Ya puedes usar los endpoints protegidos
                            """)
                        .version("1.0.0 - Sprint 4")
                        .contact(new Contact()
                                .name("Equipo Plantopolis")
                                .email("plantopolis@equipo.com"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))

                // ── Servidores disponibles ────────────────────────────
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Servidor local de desarrollo"),
                        new Server()
                                .url("http://localhost:8080")
                                .description("Servidor Docker")))

                // ── Seguridad JWT global ──────────────────────────────
                .addSecurityItem(new SecurityRequirement()
                        .addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("""
                                            Token JWT obtenido en POST /api/auth/login.
                                            Formato: Bearer {token}
                                            """)));
    }
}