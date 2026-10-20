package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.LogSistema;
import com.plantopolis.backend.domain.model.ResumenLogsSistema;
import com.plantopolis.backend.domain.port.in.ConsultarLogsSistemaUseCase;
import com.plantopolis.backend.domain.port.out.LogSistemaRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio de aplicación que gestiona la consulta, consolidación y filtrado de los logs del sistema.
 * Unifica los registros de auditoría persistidos en base de datos con las acciones HTTP y eventos
 * del motor capturados en tiempo real para ofrecer trazabilidad completa al administrador.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLogSistemaService implements ConsultarLogsSistemaUseCase {

    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");

    private final LogSistemaRepositoryPort logRepositoryPort;

    /**
     * Consulta y filtra los logs del sistema, calculando además las métricas de resumen (KPIs).
     *
     * @param busqueda Texto libre de búsqueda (filtra por acción, descripción, usuario, rol o endpoint).
     * @param modulo   Módulo específico a filtrar (opcional).
     * @param nivel    Nivel de severidad a filtrar (opcional).
     * @param origen   Fuente del registro a filtrar (opcional).
     * @param limite   Límite máximo de registros a devolver (por defecto 300).
     * @return {@link ResumenLogsSistema} con contadores globales y listado filtrado ordenado por fecha descendente.
     */
    @Override
    public ResumenLogsSistema consultarLogs(String busqueda, String modulo, String nivel, String origen, int limite) {
        int maxResultados = (limite <= 0 || limite > 1000) ? 300 : limite;

        // 1. Recolectar eventos en tiempo real + auditoría histórica de base de datos
        List<LogSistema> todosLosLogs = new ArrayList<>();
        todosLosLogs.addAll(logRepositoryPort.obtenerLogsEnTiempoReal());
        todosLosLogs.addAll(logRepositoryPort.obtenerLogsAuditoriaBaseDatos());

        // Ordenar del más reciente al más antiguo
        todosLosLogs.sort(Comparator.comparing(
                (LogSistema l) -> l.getFechaHora() != null ? l.getFechaHora() : LocalDateTime.MIN
        ).reversed());

        // 2. Calcular métricas globales antes de recortar por límite (o sobre el conjunto activo)
        long totalInfo = todosLosLogs.stream().filter(l -> "INFO".equalsIgnoreCase(l.getNivel())).count();
        long totalSuccess = todosLosLogs.stream().filter(l -> "SUCCESS".equalsIgnoreCase(l.getNivel())).count();
        long totalWarn = todosLosLogs.stream().filter(l -> "WARN".equalsIgnoreCase(l.getNivel())).count();
        long totalError = todosLosLogs.stream().filter(l -> "ERROR".equalsIgnoreCase(l.getNivel())).count();

        Map<String, Long> porModulo = todosLosLogs.stream()
                .filter(l -> l.getModulo() != null)
                .collect(Collectors.groupingBy(LogSistema::getModulo, LinkedHashMap::new, Collectors.counting()));

        // 3. Aplicar filtros solicitados por el administrador
        String query = busqueda != null ? busqueda.trim().toLowerCase(Locale.ROOT) : "";
        String modFiltro = modulo != null ? modulo.trim().toUpperCase(Locale.ROOT) : "";
        String nivFiltro = nivel != null ? nivel.trim().toUpperCase(Locale.ROOT) : "";
        String oriFiltro = origen != null ? origen.trim().toUpperCase(Locale.ROOT) : "";

        List<LogSistema> filtrados = todosLosLogs.stream()
                .filter(l -> modFiltro.isEmpty() || modFiltro.equalsIgnoreCase(l.getModulo()))
                .filter(l -> nivFiltro.isEmpty() || nivFiltro.equalsIgnoreCase(l.getNivel()))
                .filter(l -> oriFiltro.isEmpty() || oriFiltro.equalsIgnoreCase(l.getOrigen()))
                .filter(l -> {
                    if (query.isEmpty()) return true;
                    return contiene(l.getAccion(), query)
                            || contiene(l.getDescripcion(), query)
                            || contiene(l.getUsuario(), query)
                            || contiene(l.getRol(), query)
                            || contiene(l.getEndpoint(), query)
                            || contiene(l.getModulo(), query)
                            || contiene(l.getId(), query);
                })
                .limit(maxResultados)
                .collect(Collectors.toList());

        return ResumenLogsSistema.builder()
                .totalEventos(todosLosLogs.size())
                .totalInfo(totalInfo)
                .totalSuccess(totalSuccess)
                .totalWarn(totalWarn)
                .totalError(totalError)
                .eventosPorModulo(porModulo)
                .logs(filtrados)
                .build();
    }

    /**
     * Registra un evento de actividad en el buffer de tiempo real asegurando zona horaria America/Bogota.
     *
     * @param evento Objeto {@link LogSistema} con los detalles de la acción.
     */
    @Override
    public void registrarEvento(LogSistema evento) {
        if (evento == null) return;
        if (evento.getFechaHora() == null) {
            evento.setFechaHora(LocalDateTime.now(ZONA_BOGOTA));
        }
        logRepositoryPort.guardarLogEnTiempoReal(evento);
    }

    /**
     * Limpia los eventos en memoria y deja constancia de la acción realizada por el administrador.
     *
     * @param usuarioAdmin Identificador del administrador que solicitó limpiar la memoria.
     */
    @Override
    public void limpiarLogsEnMemoria(String usuarioAdmin) {
        logRepositoryPort.limpiarBufferTiempoReal();
        logRepositoryPort.guardarLogEnTiempoReal(LogSistema.builder()
                .fechaHora(LocalDateTime.now(ZONA_BOGOTA))
                .nivel("WARN")
                .modulo("SISTEMA")
                .accion("LIMPIEZA_LOGS_MEMORIA")
                .descripcion("El administrador " + usuarioAdmin + " limpió el buffer de logs en tiempo real. Los registros históricos de auditoría en BD se mantienen intactos.")
                .usuario(usuarioAdmin)
                .rol("ADMINISTRADOR")
                .metodoHttp("DELETE")
                .endpoint("/api/admin/logs/memoria")
                .codigoEstado(200)
                .duracionMs(1L)
                .origen("API_HTTP")
                .build());
    }

    /**
     * Verifica de forma segura si un campo de texto contiene la subcadena buscada.
     *
     * @param texto Valor del campo a evaluar.
     * @param query Subcadena en minúsculas.
     * @return true si el campo no es nulo y contiene la búsqueda.
     */
    private boolean contiene(String texto, String query) {
        return texto != null && texto.toLowerCase(Locale.ROOT).contains(query);
    }
}
