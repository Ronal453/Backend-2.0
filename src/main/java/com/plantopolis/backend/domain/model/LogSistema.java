package com.plantopolis.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Modelo de dominio que representa un registro de actividad o evento (log) del sistema.
 * Consolida tanto las acciones operativas persistidas en base de datos como las peticiones
 * HTTP interceptadas en tiempo real y los eventos internos del motor de aplicación.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogSistema {

    /** Identificador único del evento de log (ej. HTTP-1024, TAREA-15, LOTE-8). */
    private String id;

    /** Fecha y hora exacta del evento normalizada a America/Bogota. */
    private LocalDateTime fechaHora;

    /** Nivel de severidad o resultado: INFO, SUCCESS, WARN, ERROR. */
    private String nivel;

    /** Módulo funcional asociado: AUTH, USUARIOS, PRODUCTOS, PEDIDOS, TAREAS, LOTES, MERMAS, ZONAS, PROVEEDORES, BACKUP, SISTEMA. */
    private String modulo;

    /** Código o nombre corto de la acción ejecutada por el sistema. */
    private String accion;

    /** Descripción legible y detallada de la operación realizada. */
    private String descripcion;

    /** Nombre o correo del usuario que ejecutó la acción, o 'SISTEMA' para procesos automáticos. */
    private String usuario;

    /** Rol del actor en el momento de la acción (ADMINISTRADOR, TRABAJADOR, CLIENTE, SISTEMA). */
    private String rol;

    /** Método HTTP o tipo de operación interna (POST, PUT, PATCH, DELETE, GET, DB_AUDIT, SLF4J). */
    private String metodoHttp;

    /** Ruta del endpoint invocado o componente interno que originó el evento. */
    private String endpoint;

    /** Código de estado HTTP resultante (ej. 200, 201, 400, 403, 500). */
    private Integer codigoEstado;

    /** Tiempo de procesamiento de la acción en milisegundos. */
    private Long duracionMs;

    /** Fuente del registro: API_HTTP, AUDITORIA_BD o MOTOR_SISTEMA. */
    private String origen;
}
