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
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class BackupJdbcAdapter implements BackupRepositoryPort {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    @Override
    public List<String> listarTablasEnOrdenDependencia() {
        // Obtenemos todas las tablas del esquema (lista blanca)
        String sqlTablas = "SELECT TABLE_NAME FROM USER_TABLES WHERE TABLE_NAME NOT LIKE 'BIN%' AND TEMPORARY = 'N' AND NESTED = 'NO'";
        List<String> tablas = jdbcTemplate.queryForList(sqlTablas, String.class);

        // Obtenemos las FK para construir grafo de dependencias
        String sqlFks = "SELECT a.table_name AS tabla_origen, c_pk.table_name AS tabla_destino " +
                "FROM user_constraints a " +
                "JOIN user_constraints c_pk ON a.r_constraint_name = c_pk.constraint_name " +
                "WHERE a.constraint_type = 'R'";
        
        List<Map<String, Object>> fks = jdbcTemplate.queryForList(sqlFks);
        
        // Algoritmo de ordenamiento topologico
        Map<String, List<String>> grafo = new LinkedHashMap<>();
        Map<String, Integer> inDegree = new LinkedHashMap<>();
        
        for (String t : tablas) {
            grafo.put(t, new ArrayList<>());
            inDegree.put(t, 0);
        }
        
        for (Map<String, Object> fk : fks) {
            String origen = (String) fk.get("TABLA_ORIGEN");
            String destino = (String) fk.get("TABLA_DESTINO");
            // El origen depende del destino (destino debe insertarse antes)
            if (tablas.contains(origen) && tablas.contains(destino) && !origen.equals(destino)) {
                grafo.get(destino).add(origen);
                inDegree.put(origen, inDegree.get(origen) + 1);
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
            // Ciclo detectado (no deberia pasar en nuestro modelo)
            log.warn("Se detecto un ciclo en las dependencias. Tablas no incluidas en orden: " + 
                     tablas.stream().filter(t -> !ordenadas.contains(t)).collect(Collectors.toList()));
            // Agregamos las faltantes al final
            for(String t : tablas) {
                if(!ordenadas.contains(t)) ordenadas.add(t);
            }
        }
        
        return ordenadas;
    }

    @Override
    public List<TablaRespaldo> contarFilasPorTabla() {
        List<String> tablas = listarTablasEnOrdenDependencia();
        List<TablaRespaldo> resultado = new ArrayList<>();
        for (String tabla : tablas) {
            Long filas = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM \"" + tabla + "\"", Long.class);
            resultado.add(new TablaRespaldo(tabla, filas == null ? 0 : filas));
        }
        return resultado;
    }

    @Override
    public ScriptRespaldo exportarDatosSql(String encabezado) {
        return transactionTemplate.execute(status -> {
            jdbcTemplate.execute("SET TRANSACTION READ ONLY"); // Snapshot constante

            List<String> tablas = listarTablasEnOrdenDependencia();
            StringBuilder sql = new StringBuilder();
            sql.append(encabezado);

            List<TablaRespaldo> reporteTablas = new ArrayList<>();
            long totalFilas = 0;

            // Primero contamos para armar el reporte
            for (String tabla : tablas) {
                Long filas = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM \"" + tabla + "\"", Long.class);
                long f = filas == null ? 0 : filas;
                reporteTablas.add(new TablaRespaldo(tabla, f));
                totalFilas += f;
                sql.append("-- @reporte ").append(tabla).append("=").append(f).append("\n");
            }
            sql.append("-- @total ").append(totalFilas).append("\n");
            sql.append("-- ============================================================\n");
            sql.append("SET DEFINE OFF;\n");

            // DELETE en orden inverso
            List<String> tablasInversas = new ArrayList<>(tablas);
            Collections.reverse(tablasInversas);
            for (String tabla : tablasInversas) {
                sql.append("DELETE FROM \"").append(tabla).append("\";\n");
            }

            // INSERT en orden normal
            for (String tabla : tablas) {
                String sqlCols = "SELECT COLUMN_NAME, DATA_TYPE FROM USER_TAB_COLS WHERE TABLE_NAME = ? AND VIRTUAL_COLUMN = 'NO' AND HIDDEN_COLUMN = 'NO' ORDER BY COLUMN_ID";
                List<Map<String, Object>> colsInfo = jdbcTemplate.queryForList(sqlCols, tabla);
                
                List<String> colNames = new ArrayList<>();
                List<String> colTypes = new ArrayList<>();
                for (Map<String, Object> c : colsInfo) {
                    colNames.add("\"" + c.get("COLUMN_NAME") + "\"");
                    String type = (String) c.get("DATA_TYPE");
                    if (type.contains("BLOB") || type.contains("RAW")) {
                        throw new IllegalStateException("Tipos binarios no soportados en la tabla " + tabla);
                    }
                    colTypes.add(type);
                }

                String pkCol = obtenerPk(tabla);
                String orderClause = pkCol != null ? " ORDER BY \"" + pkCol + "\"" : "";
                
                String selectQuery = "SELECT * FROM \"" + tabla + "\"" + orderClause;
                jdbcTemplate.query(selectQuery, rs -> {
                    StringBuilder insert = new StringBuilder("INSERT INTO \"").append(tabla).append("\" (");
                    insert.append(String.join(",", colNames)).append(") VALUES (");
                    
                    for (int i = 0; i < colTypes.size(); i++) {
                        if (i > 0) insert.append(",");
                        String tipo = colTypes.get(i);
                        Object valor = rs.getObject(i + 1);
                        insert.append(OracleSqlLiteralFormatter.format(valor, tipo));
                    }
                    insert.append(");\n");
                    sql.append(insert.toString());
                });
            }

            sql.append("COMMIT;\n");

            // Reajuste de secuencias de identidad
            for (String tabla : tablas) {
                String sqlIdent = "SELECT COLUMN_NAME FROM USER_TAB_IDENTITY_COLS WHERE TABLE_NAME = ?";
                List<String> identCols = jdbcTemplate.queryForList(sqlIdent, String.class, tabla);
                for (String col : identCols) {
                    sql.append("ALTER TABLE \"").append(tabla).append("\" MODIFY (\"").append(col).append("\" GENERATED BY DEFAULT AS IDENTITY (START WITH LIMIT VALUE));\n");
                }
            }

            return ScriptRespaldo.builder()
                    .sql(sql.toString())
                    .tablas(reporteTablas)
                    .totalFilas(totalFilas)
                    .build();
        });
    }

    private String obtenerPk(String tabla) {
        String sql = "SELECT cols.column_name FROM user_constraints cons, user_cons_columns cols " +
                     "WHERE cons.constraint_type = 'P' AND cons.constraint_name = cols.constraint_name " +
                     "AND cons.table_name = ?";
        List<String> pks = jdbcTemplate.queryForList(sql, String.class, tabla);
        return pks.isEmpty() ? null : pks.get(0);
    }

    @Override
    public ResultadoRestauracionDatos restaurarDatos(List<SentenciaRestauracion> sentencias) {
        List<String> tablasNormal = listarTablasEnOrdenDependencia();
        List<String> advertencias = new ArrayList<>();
        
        List<SentenciaRestauracion> inserts = sentencias.stream()
            .filter(s -> s.getSql().startsWith("INSERT"))
            .collect(Collectors.toList());

        transactionTemplate.execute(status -> {
            // 1. DELETE inverso
            List<String> tablasInversas = new ArrayList<>(tablasNormal);
            Collections.reverse(tablasInversas);
            for (String tabla : tablasInversas) {
                jdbcTemplate.update("DELETE FROM \"" + tabla + "\"");
            }
            
            // 2. Ejecutar INSERTs en lotes
            int batchSize = 500;
            for (int i = 0; i < inserts.size(); i += batchSize) {
                int end = Math.min(i + batchSize, inserts.size());
                List<SentenciaRestauracion> batch = inserts.subList(i, end);
                String[] batchSql = batch.stream().map(s -> {
                    String sql = s.getSql();
                    // Reemplazar fechas con T (ej: 2024-10-08T16:11:11) - SOLO si no estan envueltas ya
                    sql = sql.replaceAll("(?<!TO_TIMESTAMP\\()(?<!TO_DATE\\()'(\\d{4}-\\d{2}-\\d{2})T(\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?)'", "TO_TIMESTAMP('$1 $2', 'YYYY-MM-DD HH24:MI:SS.FF')");
                    // Reemplazar fechas con espacio (ej: 2024-10-08 16:11:11) - SOLO si no estan envueltas ya
                    sql = sql.replaceAll("(?<!TO_TIMESTAMP\\()(?<!TO_DATE\\()'(\\d{4}-\\d{2}-\\d{2}) (\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?)'", "TO_TIMESTAMP('$1 $2', 'YYYY-MM-DD HH24:MI:SS.FF')");
                    return sql;
                }).toArray(String[]::new);
                try {
                    jdbcTemplate.batchUpdate(batchSql);
                } catch (Exception e) {
                    log.error("Error executing batch update! First statement in batch: {}", batchSql.length > 0 ? batchSql[0] : "empty");
                    for(String failSql : batchSql) {
                        log.error("BATCH SQL: {}", failSql);
                    }
                    throw e;
                }
            }
            
            // 2.5 Limpiar tokens después de la restauración para evitar lockout de seguridad (doble sesión)
            jdbcTemplate.update("UPDATE \"USUARIO\" SET \"ULTIMO_TOKEN\" = NULL");
            
            return null; // Commit automatico al salir del template
        });

        // 3. Reajuste de secuencias IDENTITY
        // (Esto no puede ir en la transaccion anterior porque ALTER TABLE hace autocommit)
        List<SentenciaRestauracion> alters = sentencias.stream()
            .filter(s -> s.getSql().startsWith("ALTER TABLE"))
            .collect(Collectors.toList());
            
        for (SentenciaRestauracion alter : alters) {
            try {
                jdbcTemplate.execute(alter.getSql());
            } catch (Exception e) {
                log.warn("Fallo al reajustar identidad en tabla {}: {}", alter.getTabla(), e.getMessage());
                advertencias.add("No se pudo reajustar la secuencia de identidad para " + alter.getTabla() + ". Los proximos inserts podrian requerir ajuste manual.");
            }
        }

        // 4. Calcular filas restauradas
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
}
