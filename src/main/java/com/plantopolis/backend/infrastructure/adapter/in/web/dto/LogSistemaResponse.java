package com.plantopolis.backend.infrastructure.adapter.in.web.dto;

import com.plantopolis.backend.domain.model.LogSistema;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para un evento individual de log o auditoría del sistema.
 *
 * @param id           Identificador único del evento.
 * @param fechaHora    Fecha y hora de ocurrencia (America/Bogota).
 * @param nivel        Nivel de severidad (INFO, SUCCESS, WARN, ERROR).
 * @param modulo       Módulo funcional asociado.
 * @param accion       Código de la acción ejecutada.
 * @param descripcion  Detalle legible de la operación.
 * @param usuario      Usuario o actor responsable.
 * @param rol          Rol del actor.
 * @param metodoHttp   Método HTTP o tipo de evento.
 * @param endpoint     Endpoint o componente origen.
 * @param codigoEstado Código de estado HTTP o de proceso.
 * @param duracionMs   Duración en milisegundos.
 * @param origen       Fuente del registro (API_HTTP, AUDITORIA_BD, MOTOR_SISTEMA).
 */
public record LogSistemaResponse(
        String id,
        LocalDateTime fechaHora,
        String nivel,
        String modulo,
        String accion,
        String descripcion,
        String usuario,
        String rol,
        String metodoHttp,
        String endpoint,
        Integer codigoEstado,
        Long duracionMs,
        String origen
) {
    /**
     * Convierte un objeto de dominio {@link LogSistema} a su representación DTO REST.
     *
     * @param domain Modelo de dominio.
     * @return Instancia de {@link LogSistemaResponse}.
     */
    public static LogSistemaResponse from(LogSistema domain) {
        return new LogSistemaResponse(
                domain.getId(),
                domain.getFechaHora(),
                domain.getNivel(),
                domain.getModulo(),
                domain.getAccion(),
                domain.getDescripcion(),
                domain.getUsuario(),
                domain.getRol(),
                domain.getMetodoHttp(),
                domain.getEndpoint(),
                domain.getCodigoEstado(),
                domain.getDuracionMs(),
                domain.getOrigen()
        );
    }
}
