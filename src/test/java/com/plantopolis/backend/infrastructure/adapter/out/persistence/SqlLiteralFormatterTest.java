package com.plantopolis.backend.infrastructure.adapter.out.persistence;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para {@link SqlLiteralFormatter}.
 * Verifica el formateo y escape seguro de literales en sentencias SQL.
 */
class SqlLiteralFormatterTest {

    @Test
    void format_Null_RetornaNULL() {
        assertEquals("NULL", SqlLiteralFormatter.format(null, "VARCHAR"));
    }

    @Test
    void format_Boolean_RetornaTrueOFalse() {
        assertEquals("TRUE", SqlLiteralFormatter.format(Boolean.TRUE, "BOOLEAN"));
        assertEquals("FALSE", SqlLiteralFormatter.format(Boolean.FALSE, "BOOLEAN"));
    }

    @Test
    void format_Number_RetornaString() {
        assertEquals("12.50", SqlLiteralFormatter.format(new BigDecimal("12.50"), "NUMERIC"));
        assertEquals("10", SqlLiteralFormatter.format(10, "INTEGER"));
    }

    @Test
    void format_StringConComillas_EscapaCorrectamente() {
        assertEquals("'L''Oreal'", SqlLiteralFormatter.format("L'Oreal", "VARCHAR"));
    }

    @Test
    void format_Timestamp_GeneraToTimestamp() {
        Timestamp ts = Timestamp.valueOf("2026-10-07 23:15:02.123");
        String formatted = SqlLiteralFormatter.format(ts, "TIMESTAMP");
        assertTrue(formatted.startsWith("TO_TIMESTAMP('2026-10-07 23:15:02.123'"));
    }
}
