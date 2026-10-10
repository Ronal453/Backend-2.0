package com.plantopolis.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class BackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

    @Bean
    public CommandLineRunner initAdmin(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        return args -> {
            String hash = passwordEncoder.encode("admin123");
            Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM \"USUARIO\" WHERE \"CORREO\" = 'admin@plantopolis.com'", Integer.class);
            if (count != null && count > 0) {
                jdbcTemplate.update("UPDATE \"USUARIO\" SET \"CONTRASENA_HASH\" = ?, \"ID_ROL\" = 1 WHERE \"CORREO\" = 'admin@plantopolis.com'", hash);
            } else {
                jdbcTemplate.update("INSERT INTO \"USUARIO\" (\"NOMBRE_COMPLETO\", \"CORREO\", \"CONTRASENA_HASH\", \"ACTIVO\", \"ID_ROL\") VALUES (?, ?, ?, ?, ?)",
                    "Administrador", "admin@plantopolis.com", hash, 1, 1);
            }
        };
    }
}
