package com.plantopolis.backend.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita el soporte para tareas programadas mediante anotaciones @Scheduled en Spring Boot.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
