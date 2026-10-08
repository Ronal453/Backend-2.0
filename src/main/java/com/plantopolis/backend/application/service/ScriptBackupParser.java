package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.SentenciaRestauracion;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parsea y valida el archivo SQL de backup.
 * Asegura que no se inyecten sentencias maliciosas o destructivas (DROP, ALTER estructurales, etc).
 */
public class ScriptBackupParser {

    private static final String FIRMA = "-- PLANTOPOLIS-BACKUP v1";
    private static final Pattern PATRON_REPORTE = Pattern.compile("^-- @reporte ([A-Z0-9_#]+)=(\\d+)$");
    private static final Pattern PATRON_INSERT = Pattern.compile("^INSERT INTO \"([A-Z0-9_#]+)\" \\(.+\\) VALUES \\(.*\\);");
    private static final Pattern PATRON_ALTER_IDENTITY = Pattern.compile("^ALTER TABLE \"([A-Z0-9_#]+)\" MODIFY \\(.+ IDENTITY .*\\);");

    @Data
    @AllArgsConstructor
    public static class ScriptParseado {
        private List<SentenciaRestauracion> sentencias;
        private Map<String, Long> reporteDeclarado;
    }

    public static ScriptParseado parsear(byte[] contenido) {
        List<SentenciaRestauracion> sentencias = new ArrayList<>();
        Map<String, Long> reporteDeclarado = new LinkedHashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(contenido), StandardCharsets.UTF_8))) {
            String linea = reader.readLine();
            
            // Quitar BOM UTF-8 si existe
            if (linea != null && linea.startsWith("\uFEFF")) {
                linea = linea.substring(1);
            }

            if (linea == null || !linea.trim().equals(FIRMA)) {
                throw new IllegalArgumentException("El archivo no es un backup valido de Plantopolis (firma incorrecta o ausente).");
            }

            int numLinea = 1;
            while ((linea = reader.readLine()) != null) {
                numLinea++;
                String lineaTrim = linea.trim();
                if (lineaTrim.isEmpty()) continue;

                if (lineaTrim.startsWith("--")) {
                    Matcher m = PATRON_REPORTE.matcher(lineaTrim);
                    if (m.matches()) {
                        reporteDeclarado.put(m.group(1), Long.parseLong(m.group(2)));
                    }
                    continue;
                }

                if (lineaTrim.equals("SET DEFINE OFF;") || lineaTrim.equals("COMMIT;")) {
                    continue; // Sentencias de control aceptadas
                }

                if (lineaTrim.startsWith("DELETE FROM \"")) {
                    continue; // Se ignoran los DELETE del script porque el adaptador hara los suyos en el orden topologico inverso correcto
                }

                Matcher mInsert = PATRON_INSERT.matcher(lineaTrim);
                if (mInsert.matches()) {
                    String tabla = mInsert.group(1);
                    String sqlSinPuntoYComa = lineaTrim.substring(0, lineaTrim.length() - 1);
                    sentencias.add(new SentenciaRestauracion(tabla, sqlSinPuntoYComa, numLinea));
                    continue;
                }

                Matcher mAlter = PATRON_ALTER_IDENTITY.matcher(lineaTrim);
                if (mAlter.matches()) {
                    String tabla = mAlter.group(1);
                    String sqlSinPuntoYComa = lineaTrim.substring(0, lineaTrim.length() - 1);
                    sentencias.add(new SentenciaRestauracion(tabla, sqlSinPuntoYComa, numLinea));
                    continue;
                }

                // Cualquier otra sentencia rechaza el script por seguridad
                throw new IllegalArgumentException("Sentencia no permitida en la linea " + numLinea + ": " + lineaTrim);
            }

        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e;
            }
            throw new IllegalArgumentException("Error al leer el archivo de backup: " + e.getMessage(), e);
        }

        return new ScriptParseado(sentencias, reporteDeclarado);
    }
}
