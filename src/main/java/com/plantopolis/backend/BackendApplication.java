package com.plantopolis.backend;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Punto de entrada principal de la aplicación Spring Boot de Plantopolis.
 */
@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

    /**
     * Inicializador de arranque que garantiza que la cuenta administrativa principal
     * ({@code admin@plantopolis.com}) exista y tenga credenciales válidas y rol ADMINISTRADOR.
     *
     * @param jdbcTemplate    Plantilla JDBC conectada a PostgreSQL.
     * @param passwordEncoder Codificador BCrypt configurado en el sistema.
     * @return Runner ejecutado tras iniciar el contexto de Spring.
     */
    @Bean
    public CommandLineRunner initAdmin(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        return args -> {
            String hash = passwordEncoder.encode("admin123");
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM usuario WHERE correo = 'admin@plantopolis.com'",
                    Integer.class
            );
            if (count != null && count > 0) {
                jdbcTemplate.update(
                        "UPDATE usuario SET contrasena_hash = ?, id_rol = 1 WHERE correo = 'admin@plantopolis.com'",
                        hash
                );
            } else {
                jdbcTemplate.update(
                        "INSERT INTO usuario (nombre_completo, correo, contrasena_hash, activo, id_rol) VALUES (?, ?, ?, ?, ?)",
                        "Administrador", "admin@plantopolis.com", hash, true, 1
                );
            }
        };
    }
}
