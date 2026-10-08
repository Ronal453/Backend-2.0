package com.plantopolis.backend.infrastructure.adapter.out.persistence;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

class OracleSqlLiteralFormatterTest {

    @Test
    void format_Null_RetornaNULL() {
        assertEquals("NULL", OracleSqlLiteralFormatter.format(null, "VARCHAR2"));
    }

    @Test
    void format_Number_RetornaString() {
        assertEquals("12.50", OracleSqlLiteralFormatter.format(new BigDecimal("12.50"), "NUMBER"));
        assertEquals("10", OracleSqlLiteralFormatter.format(10, "NUMBER"));
    }

    @Test
    void format_StringConComillas_EscapaCorrectamente() {
        assertEquals("'L''Oreal'", OracleSqlLiteralFormatter.format("L'Oreal", "VARCHAR2"));
    }

    @Test
    void format_Timestamp_GeneraToTimestamp() {
        Timestamp ts = Timestamp.valueOf("2026-10-07 23:15:02.123");
        String formatted = OracleSqlLiteralFormatter.format(ts, "TIMESTAMP");
        assertTrue(formatted.startsWith("TO_TIMESTAMP('2026-10-07 23:15:02.123'"));
    }
}
