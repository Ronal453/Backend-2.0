package com.plantopolis.backend.infrastructure.adapter.out.persistence;

import com.plantopolis.backend.domain.model.ResultadoRestauracionDatos;
import com.plantopolis.backend.domain.model.ScriptRespaldo;
import com.plantopolis.backend.domain.model.SentenciaRestauracion;
import com.plantopolis.backend.domain.model.TablaRespaldo;
import com.plantopolis.backend.domain.port.out.BackupRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Adaptador JDBC para exportación y restauración de copias de seguridad en PostgreSQL.
 * <p>
 * Consulta el catálogo estándar {@code information_schema} de PostgreSQL para construir
 * el orden topológico de tablas según sus llaves foráneas, exportar los registros en formato
 * firmado {@code PLANTOPOLIS-BACKUP v1} y restaurar tanto respaldos generados en PostgreSQL
 * como respaldos históricos provenientes de Oracle.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BackupJdbcAdapter implements BackupRepositoryPort {

    private static final Pattern PATRON_ENCABEZADO_INSERT =
            Pattern.compile("^INSERT INTO \"([A-Za-z0-9_#]+)\" \\(([^)]+)\\) VALUES ", Pattern.CASE_INSENSITIVE);

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    /**
     * Obtiene las tablas del esquema {@code public} de PostgreSQL ordenadas topológicamente
     * según sus llaves foráneas (primero las tablas padre sin dependencias, luego las hijas).
     *
     * @return Lista de nombres de tablas en mayúsculas en orden seguro de inserción.
     */
    @Override
    public List<String> listarTablasEnOrdenDependencia() {
        String sqlTablas = """
            SELECT UPPER(table_name)
            FROM information_schema.tables
            WHERE table_schema = 'public'
              AND table_type = 'BASE TABLE'
            ORDER BY table_name
        """;
        List<String> tablas = jdbcTemplate.queryForList(sqlTablas, String.class);

        String sqlFks = """
            SELECT DISTINCT
                   UPPER(tc.table_name) AS tabla_origen,
                   UPPER(ccu.table_name) AS tabla_destino
            FROM information_schema.table_constraints AS tc
            JOIN information_schema.constraint_column_usage AS ccu
              ON ccu.constraint_name = tc.constraint_name
             AND ccu.table_schema = tc.table_schema
            WHERE tc.constraint_type = 'FOREIGN KEY'
              AND tc.table_schema = 'public'
        """;

        List<Map<String, Object>> fks = jdbcTemplate.queryForList(sqlFks);

        Map<String, List<String>> grafo = new LinkedHashMap<>();
        Map<String, Integer> inDegree = new LinkedHashMap<>();

        for (String t : tablas) {
            grafo.put(t, new ArrayList<>());
            inDegree.put(t, 0);
        }

        for (Map<String, Object> fk : fks) {
            String origen = obtenerValorMapa(fk, "tabla_origen");
            String destino = obtenerValorMapa(fk, "tabla_destino");
            if (origen != null && destino != null
                    && tablas.contains(origen) && tablas.contains(destino)
                    && !origen.equals(destino)) {
                if (!grafo.get(destino).contains(origen)) {
                    grafo.get(destino).add(origen);
                    inDegree.put(origen, inDegree.get(origen) + 1);
                }
            }
        }

        List<String> ordenadas = new ArrayList<>();
        List<String> queue = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        while (!queue.isEmpty()) {
            String actual = queue.remove(0);
            ordenadas.add(actual);

            for (String vecino : grafo.get(actual)) {
                inDegree.put(vecino, inDegree.get(vecino) - 1);
                if (inDegree.get(vecino) == 0) {
                    queue.add(vecino);
                }
            }
        }

        if (ordenadas.size() != tablas.size()) {
            log.warn("Se detectó un ciclo en las dependencias. Tablas añadidas al final: {}",
                    tablas.stream().filter(t -> !ordenadas.contains(t)).collect(Collectors.toList()));
            for (String t : tablas) {
                if (!ordenadas.contains(t)) {
                    ordenadas.add(t);
                }
            }
        }

        return ordenadas;
    }

    /**
     * Cuenta el número de registros actuales en cada tabla de la base de datos.
     *
     * @return Lista de {@link TablaRespaldo} con nombre de tabla y conteo de filas.
     */
    @Override
    public List<TablaRespaldo> contarFilasPorTabla() {
        List<String> tablas = listarTablasEnOrdenDependencia();
        List<TablaRespaldo> resultado = new ArrayList<>();
        for (String tabla : tablas) {
            Long filas = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tabla, Long.class);
            resultado.add(new TablaRespaldo(tabla, filas == null ? 0 : filas));
        }
        return resultado;
    }

    /**
     * Genera el script SQL completo de respaldo dentro de una transacción de solo lectura.
     *
     * @param encabezado Comentarios de cabecera con metadatos de fecha y autor.
     * @return Objeto {@link ScriptRespaldo} con el contenido SQL y el resumen por tabla.
     */
    @Override
    public ScriptRespaldo exportarDatosSql(String encabezado) {
        return transactionTemplate.execute(status -> {
            jdbcTemplate.execute("SET TRANSACTION READ ONLY");

            List<String> tablas = listarTablasEnOrdenDependencia();
            StringBuilder sql = new StringBuilder();
            sql.append(encabezado);

            List<TablaRespaldo> reporteTablas = new ArrayList<>();
            long totalFilas = 0;

            for (String tabla : tablas) {
                Long filas = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tabla, Long.class);
                long f = filas == null ? 0 : filas;
                reporteTablas.add(new TablaRespaldo(tabla, f));
                totalFilas += f;
                sql.append("-- @reporte ").append(tabla).append("=").append(f).append("\n");
            }
            sql.append("-- @total ").append(totalFilas).append("\n");
            sql.append("-- ============================================================\n");
            sql.append("SET DEFINE OFF;\n");

            List<String> tablasInversas = new ArrayList<>(tablas);
            Collections.reverse(tablasInversas);
            for (String tabla : tablasInversas) {
                sql.append("DELETE FROM \"").append(tabla).append("\";\n");
            }

            String sqlCols = """
                SELECT UPPER(column_name) AS column_name,
                       UPPER(data_type) AS data_type
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND UPPER(table_name) = ?
                ORDER BY ordinal_position
            """;

            for (String tabla : tablas) {
                List<Map<String, Object>> colsInfo = jdbcTemplate.queryForList(sqlCols, tabla);

                List<String> colNames = new ArrayList<>();
                List<String> colTypes = new ArrayList<>();
                for (Map<String, Object> c : colsInfo) {
                    String colName = obtenerValorMapa(c, "column_name");
                    String type = obtenerValorMapa(c, "data_type");
                    colNames.add("\"" + colName + "\"");
                    if (type != null && (type.contains("BYTEA") || type.contains("BLOB"))) {
                        throw new IllegalStateException("Tipos binarios no soportados en la tabla " + tabla);
                    }
                    colTypes.add(type != null ? type : "VARCHAR");
                }

                String pkCol = obtenerPk(tabla);
                String orderClause = pkCol != null ? " ORDER BY " + pkCol : "";

                String selectQuery = "SELECT * FROM " + tabla + orderClause;
                jdbcTemplate.query(selectQuery, rs -> {
                    StringBuilder insert = new StringBuilder("INSERT INTO \"").append(tabla).append("\" (");
                    insert.append(String.join(",", colNames)).append(") VALUES (");

                    for (int i = 0; i < colTypes.size(); i++) {
                        if (i > 0) insert.append(",");
                        String tipo = colTypes.get(i);
                        Object valor = rs.getObject(i + 1);
                        insert.append(SqlLiteralFormatter.format(valor, tipo));
                    }
                    insert.append(");\n");
                    sql.append(insert);
                });
            }

            sql.append("COMMIT;\n");

            String sqlIdent = """
                SELECT UPPER(column_name)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND UPPER(table_name) = ?
                  AND (is_identity = 'YES' OR column_default LIKE 'nextval%')
            """;
            for (String tabla : tablas) {
                List<String> identCols = jdbcTemplate.queryForList(sqlIdent, String.class, tabla);
                for (String col : identCols) {
                    sql.append("ALTER TABLE \"").append(tabla).append("\" MODIFY (\"")
                       .append(col).append("\" GENERATED BY DEFAULT AS IDENTITY (START WITH LIMIT VALUE));\n");
                }
            }

            return ScriptRespaldo.builder()
                    .sql(sql.toString())
                    .tablas(reporteTablas)
                    .totalFilas(totalFilas)
                    .build();
        });
    }

    /**
     * Obtiene el nombre de la columna clave primaria para ordenar los registros exportados.
     */
    private String obtenerPk(String tabla) {
        String sql = """
            SELECT UPPER(kcu.column_name)
            FROM information_schema.table_constraints tc
            JOIN information_schema.key_column_usage kcu
              ON tc.constraint_name = kcu.constraint_name
             AND tc.table_schema = kcu.table_schema
            WHERE tc.constraint_type = 'PRIMARY KEY'
              AND tc.table_schema = 'public'
              AND UPPER(tc.table_name) = ?
            ORDER BY kcu.ordinal_position
        """;
        List<String> pks = jdbcTemplate.queryForList(sql, String.class, tabla);
        return pks.isEmpty() ? null : pks.get(0);
    }

    /**
     * Restaura los datos de un script de respaldo sobre PostgreSQL en una transacción atómica,
     * limpiando en orden inverso, insertando en orden topológico y sincronizando las secuencias IDENTITY.
     *
     * @param sentencias Lista de sentencias parseadas y validadas.
     * @return Resumen de filas restauradas y advertencias si las hubo.
     */
    @Override
    public ResultadoRestauracionDatos restaurarDatos(List<SentenciaRestauracion> sentencias) {
        List<String> tablasNormal = listarTablasEnOrdenDependencia();
        List<String> advertencias = new ArrayList<>();

        List<SentenciaRestauracion> inserts = sentencias.stream()
                .filter(s -> s.getSql().startsWith("INSERT"))
                .collect(Collectors.toList());

        transactionTemplate.execute(status -> {
            // 1. Eliminar datos actuales en orden topológico inverso
            List<String> tablasInversas = new ArrayList<>(tablasNormal);
            Collections.reverse(tablasInversas);
            for (String tabla : tablasInversas) {
                jdbcTemplate.update("DELETE FROM " + tabla);
            }

            // 2. Ejecutar INSERTs en lotes normalizando sintaxis hacia PostgreSQL
            int batchSize = 500;
            for (int i = 0; i < inserts.size(); i += batchSize) {
                int end = Math.min(i + batchSize, inserts.size());
                List<SentenciaRestauracion> batch = inserts.subList(i, end);
                String[] batchSql = batch.stream()
                        .map(s -> normalizarInsertParaPostgres(s.getSql()))
                        .toArray(String[]::new);
                try {
                    jdbcTemplate.batchUpdate(batchSql);
                } catch (Exception e) {
                    log.error("Error ejecutando lote de restauración. Primera sentencia: {}",
                            batchSql.length > 0 ? batchSql[0] : "vacío");
                    for (String failSql : batchSql) {
                        log.error("SQL FALLIDO: {}", failSql);
                    }
                    throw e;
                }
            }

            // 2.5 Limpiar tokens tras la restauración para evitar bloqueo por doble sesión
            jdbcTemplate.update("UPDATE usuario SET ultimo_token = NULL");

            // 3. Reajustar secuencias de identidad en PostgreSQL según el valor máximo actual de cada tabla
            sincronizarSecuenciasPostgres(tablasNormal, advertencias);

            return null;
        });

        Map<String, Long> conteo = inserts.stream()
                .collect(Collectors.groupingBy(SentenciaRestauracion::getTabla, Collectors.counting()));

        List<TablaRespaldo> tablasResp = conteo.entrySet().stream()
                .map(e -> new TablaRespaldo(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        return ResultadoRestauracionDatos.builder()
                .tablas(tablasResp)
                .totalFilas(inserts.size())
                .advertencias(advertencias)
                .build();
    }

    /**
     * Transforma una sentencia INSERT del formato de backup (con identificadores entrecomillados
     * y funciones TO_TIMESTAMP / TO_DATE / TO_CLOB) a sintaxis nativa de PostgreSQL.
     *
     * @param sql Sentencia INSERT original.
     * @return Sentencia INSERT compatible con PostgreSQL.
     */
    String normalizarInsertParaPostgres(String sql) {
        String resultado = sql;

        // Quitar comillas dobles de la tabla y lista de columnas para que PostgreSQL resuelva sin sensibilidad a mayúsculas
        Matcher m = PATRON_ENCABEZADO_INSERT.matcher(resultado);
        if (m.find()) {
            String tabla = m.group(1).toLowerCase(Locale.ROOT);
            String columnas = m.group(2).replace("\"", "").toLowerCase(Locale.ROOT);
            resultado = "INSERT INTO " + tabla + " (" + columnas + ") VALUES " + resultado.substring(m.end());
        }

        // Convertir TO_TIMESTAMP('2026-09-04 18:04:46.670','YYYY-MM-DD HH24:MI:SS.FF') -> '2026-09-04 18:04:46.670'
        resultado = resultado.replaceAll(
                "TO_TIMESTAMP\\('([^']+)'\\s*,\\s*'[^']+'\\)",
                "'$1'"
        );

        // Convertir TO_DATE('2026-09-04','YYYY-MM-DD') -> '2026-09-04'
        resultado = resultado.replaceAll(
                "TO_DATE\\('([^']+)'\\s*,\\s*'[^']+'\\)",
                "'$1'"
        );

        // Convertir TO_CLOB('texto') -> ('texto') y EMPTY_CLOB() -> ''
        resultado = resultado.replace("TO_CLOB(", "(");
        resultado = resultado.replace("EMPTY_CLOB()", "''");

        return resultado;
    }

    /**
     * Sincroniza todas las secuencias IDENTITY de PostgreSQL al valor máximo actual de cada tabla.
     */
    private void sincronizarSecuenciasPostgres(List<String> tablas, List<String> advertencias) {
        String sqlIdent = """
            SELECT LOWER(column_name)
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND UPPER(table_name) = ?
              AND (is_identity = 'YES' OR column_default LIKE 'nextval%')
        """;

        for (String tablaUpper : tablas) {
            String tablaLower = tablaUpper.toLowerCase(Locale.ROOT);
            List<String> identCols = jdbcTemplate.queryForList(sqlIdent, String.class, tablaUpper);
            for (String colLower : identCols) {
                try {
                    String setvalSql = String.format(
                            "SELECT setval(pg_get_serial_sequence('%s', '%s'), COALESCE((SELECT MAX(%s) FROM %s), 1), (SELECT MAX(%s) IS NOT NULL FROM %s))",
                            tablaLower, colLower, colLower, tablaLower, colLower, tablaLower
                    );
                    jdbcTemplate.queryForObject(setvalSql, Long.class);
                } catch (Exception e) {
                    log.warn("Fallo al reajustar secuencia en tabla {}.{}: {}", tablaUpper, colLower, e.getMessage());
                    advertencias.add("No se pudo reajustar la secuencia de identidad para " + tablaUpper + ".");
                }
            }
        }
    }

    /**
     * Obtiene un valor de tipo String desde un mapa de resultados JDBC sin importar mayúsculas/minúsculas de la clave.
     */
    private String obtenerValorMapa(Map<String, Object> mapa, String claveLower) {
        Object val = mapa.get(claveLower);
        if (val == null) {
            val = mapa.get(claveLower.toUpperCase(Locale.ROOT));
        }
        return val != null ? val.toString() : null;
    }
}
