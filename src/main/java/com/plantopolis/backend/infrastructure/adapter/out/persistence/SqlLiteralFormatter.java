package com.plantopolis.backend.infrastructure.adapter.out.persistence;

import java.math.BigDecimal;
import java.sql.Clob;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Formateador de literales SQL para exportación de copias de seguridad de datos.
 * <p>
 * Genera literales SQL compatibles con el estándar de respaldo firmado de Plantopolis
 * ({@code PLANTOPOLIS-BACKUP v1}) y soporta tipos de PostgreSQL (Boolean, Timestamp, Date, Text)
 * así como compatibilidad con volcados históricos.
 * </p>
 */
public class SqlLiteralFormatter {

    /**
     * Constructor privado para evitar instanciación de clase utilitaria.
     */
    private SqlLiteralFormatter() {
    }

    /**
     * Convierte un valor obtenido de {@link java.sql.ResultSet} a su representación literal SQL segura.
     *
     * @param valor  Valor de la celda (puede ser {@code null}).
     * @param tipoDB Nombre del tipo de dato reportado por el motor de base de datos.
     * @return Cadena con el literal SQL escapado.
     */
    public static String format(Object valor, String tipoDB) {
        if (valor == null) {
            return "NULL";
        }

        if (valor instanceof Boolean) {
            return ((Boolean) valor) ? "TRUE" : "FALSE";
        }

        if (valor instanceof Number) {
            if (valor instanceof BigDecimal) {
                return ((BigDecimal) valor).toPlainString();
            }
            return valor.toString();
        }

        if (valor instanceof Timestamp) {
            Timestamp ts = (Timestamp) valor;
            String f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(ts);
            return "TO_TIMESTAMP('" + f + "','YYYY-MM-DD HH24:MI:SS.FF')";
        }

        if (valor instanceof java.time.LocalDateTime) {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
            String f = ((java.time.LocalDateTime) valor).format(formatter);
            return "TO_TIMESTAMP('" + f + "','YYYY-MM-DD HH24:MI:SS.FF')";
        }

        if (valor instanceof java.time.LocalDate) {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String f = ((java.time.LocalDate) valor).format(formatter);
            return "TO_DATE('" + f + "','YYYY-MM-DD')";
        }

        if (valor instanceof java.time.ZonedDateTime) {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
            String f = ((java.time.ZonedDateTime) valor).format(formatter);
            return "TO_TIMESTAMP('" + f + "','YYYY-MM-DD HH24:MI:SS.FF')";
        }

        if (valor instanceof Date) {
            if (valor instanceof java.sql.Date) {
                String d = new SimpleDateFormat("yyyy-MM-dd").format((Date) valor);
                return "TO_DATE('" + d + "','YYYY-MM-DD')";
            }
            String d = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Date) valor);
            return "TO_DATE('" + d + "','YYYY-MM-DD HH24:MI:SS')";
        }

        String text;
        if (valor instanceof Clob) {
            try {
                Clob clob = (Clob) valor;
                text = clob.getSubString(1, (int) clob.length());
            } catch (Exception e) {
                return "NULL";
            }
        } else {
            text = valor.toString();
        }

        if (tipoDB != null && tipoDB.contains("CLOB")) {
            return formatClob(text);
        } else {
            return formatString(text);
        }
    }

    /**
     * Escapa comillas simples y saltos de línea para literales de texto SQL.
     *
     * @param text Texto a escapar.
     * @return Literal SQL de texto entrecomillado.
     */
    private static String formatString(String text) {
        if (text.isEmpty()) return "NULL";
        String escaped = text.replace("'", "''");
        escaped = escaped.replace("\r\n", "' || CHR(13) || CHR(10) || '");
        escaped = escaped.replace("\n", "' || CHR(10) || '");
        return "'" + escaped + "'";
    }

    /**
     * Formatea cadenas extensas tipo CLOB en bloques concatenados.
     *
     * @param text Texto extenso.
     * @return Expresión SQL compatible para cadenas extensas.
     */
    private static String formatClob(String text) {
        if (text == null || text.isEmpty()) return "EMPTY_CLOB()";

        int maxLen = 1000;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i += maxLen) {
            int end = Math.min(i + maxLen, text.length());
            String chunk = text.substring(i, end);
            if (i > 0) sb.append(" || ");
            sb.append("TO_CLOB(").append(formatString(chunk)).append(")");
        }
        return sb.toString();
    }
}
