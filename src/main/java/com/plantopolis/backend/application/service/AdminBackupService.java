package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.ArchivoBackup;
import com.plantopolis.backend.domain.model.ResultadoRestauracion;
import com.plantopolis.backend.domain.model.ResultadoRestauracionDatos;
import com.plantopolis.backend.domain.model.ScriptRespaldo;
import com.plantopolis.backend.domain.model.SentenciaRestauracion;
import com.plantopolis.backend.domain.model.TablaRespaldo;
import com.plantopolis.backend.domain.port.in.GestionarBackupAdminUseCase;
import com.plantopolis.backend.domain.port.out.BackupRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBackupService implements GestionarBackupAdminUseCase {

    private final BackupRepositoryPort backupRepositoryPort;

    @Override
    public ArchivoBackup generarBackup(String correoAdmin) {
        log.info("Generando backup completo por {}", correoAdmin);
        LocalDateTime ahora = LocalDateTime.now(ZoneId.of("America/Bogota"));
        String fechaFormat = ahora.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String fileNameDate = ahora.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        StringBuilder encabezado = new StringBuilder();
        encabezado.append("-- PLANTOPOLIS-BACKUP v1\n");
        encabezado.append("-- Fecha de generacion : ").append(fechaFormat).append(" (America/Bogota)\n");
        encabezado.append("-- Generado por        : ").append(correoAdmin).append("\n");
        encabezado.append("-- ============================================================\n");
        encabezado.append("-- REPORTE DE RESPALDO\n");

        ScriptRespaldo script = backupRepositoryPort.exportarDatosSql(encabezado.toString());

        String nombreArchivo = "plantopolis_backup_" + fileNameDate + ".sql";

        return ArchivoBackup.builder()
                .nombreArchivo(nombreArchivo)
                .contenido(script.getSql().getBytes(StandardCharsets.UTF_8))
                .fechaGeneracion(ahora)
                .tablas(script.getTablas())
                .totalFilas(script.getTotalFilas())
                .build();
    }

    @Override
    public ResultadoRestauracion restaurarBackup(String nombreArchivo, byte[] contenido, String correoAdmin) {
        log.warn("Restauracion de BD iniciada por {} usando archivo {}", correoAdmin, nombreArchivo);
        if (contenido == null || contenido.length == 0) {
            throw new IllegalArgumentException("El archivo de backup esta vacio.");
        }
        if (!nombreArchivo.toLowerCase().endsWith(".sql")) {
            throw new IllegalArgumentException("El archivo debe tener extension .sql");
        }

        long inicio = System.currentTimeMillis();

        ScriptBackupParser.ScriptParseado parseado = ScriptBackupParser.parsear(contenido);
        List<SentenciaRestauracion> sentencias = parseado.getSentencias();
        Map<String, Long> reporteDeclarado = parseado.getReporteDeclarado();

        if (!reporteDeclarado.containsKey("ROL") || !reporteDeclarado.containsKey("USUARIO")) {
            throw new IllegalArgumentException("El backup no contiene informacion de roles y usuarios (riesgo de bloqueo total). Restauracion abortada.");
        }

        // Validar que el numero de inserts coincida con el reporte declarado
        Map<String, Long> conteoInserts = sentencias.stream()
                .filter(s -> s.getSql().startsWith("INSERT"))
                .collect(Collectors.groupingBy(SentenciaRestauracion::getTabla, Collectors.counting()));

        java.util.List<String> validacionAdvertencias = new java.util.ArrayList<>();
        for (Map.Entry<String, Long> entry : reporteDeclarado.entrySet()) {
            String tabla = entry.getKey();
            long esperadas = entry.getValue();
            long encontradas = conteoInserts.getOrDefault(tabla, 0L);
            if (esperadas != encontradas) {
                validacionAdvertencias.add(String.format("Inconsistencia en tabla %s: el reporte indica %d filas, pero hay %d (archivo modificado).", tabla, esperadas, encontradas));
                log.warn("Inconsistencia en backup {}: {}", nombreArchivo, validacionAdvertencias.get(validacionAdvertencias.size()-1));
            }
        }

        // Delegar al puerto
        ResultadoRestauracionDatos resultadoDatos = backupRepositoryPort.restaurarDatos(sentencias);
        if (resultadoDatos.getAdvertencias() != null) {
            validacionAdvertencias.addAll(resultadoDatos.getAdvertencias());
        }

        long duracion = System.currentTimeMillis() - inicio;
        log.info("Restauracion finalizada en {} ms. Filas: {}", duracion, resultadoDatos.getTotalFilas());

        return ResultadoRestauracion.builder()
                .nombreArchivo(nombreArchivo)
                .fechaRestauracion(LocalDateTime.now(ZoneId.of("America/Bogota")))
                .tablas(resultadoDatos.getTablas())
                .totalFilas(resultadoDatos.getTotalFilas())
                .advertencias(validacionAdvertencias)
                .duracionMs(duracion)
                .build();
    }

    @Override
    public List<TablaRespaldo> obtenerResumenActual() {
        return backupRepositoryPort.contarFilasPorTabla();
    }
}
