package com.plantopolis.backend.infrastructure.adapter.out.persistence;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.sql.Clob;

public class OracleSqlLiteralFormatter {

    public static String format(Object valor, String tipoDB) {
        if (valor == null) {
            return "NULL";
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
            String d = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Date) valor);
            return "TO_DATE('" + d + "','YYYY-MM-DD HH24:MI:SS')";
        }

        if (valor.getClass().getName().equals("oracle.sql.TIMESTAMP")) {
            try {
                // oracle.sql.TIMESTAMP.timestampValue() devuelve java.sql.Timestamp
                java.sql.Timestamp ts = (java.sql.Timestamp) valor.getClass().getMethod("timestampValue").invoke(valor);
                String f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(ts);
                return "TO_TIMESTAMP('" + f + "','YYYY-MM-DD HH24:MI:SS.FF')";
            } catch (Exception e) {
                // fallback
            }
        }
        
        String text = "";
        if (valor instanceof Clob) {
            try {
                Clob clob = (Clob) valor;
                text = clob.getSubString(1, (int) clob.length());
            } catch (Exception e) {
                return "EMPTY_CLOB()";
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
    
    private static String formatString(String text) {
        if (text.isEmpty()) return "NULL";
        String escaped = text.replace("'", "''");
        // Reemplazar saltos de linea por CHR(10)
        escaped = escaped.replace("\r\n", "' || CHR(13) || CHR(10) || '");
        escaped = escaped.replace("\n", "' || CHR(10) || '");
        return "'" + escaped + "'";
    }
    
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
