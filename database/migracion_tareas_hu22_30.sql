-- ==============================================================================
-- MIGRACIÓN TAREAS HU22 - HU30 (Plantopolis 2.0)
-- Agrega columna DESCRIPCION opcional a TAREA y TAREARECURRENTE
-- e índice de búsqueda para tareas recurrentes
-- ==============================================================================

DECLARE
    v_col_exists NUMBER;
BEGIN
    -- 1. Agregar columna DESCRIPCION a TAREA si no existe
    SELECT COUNT(*) INTO v_col_exists
    FROM user_tab_columns
    WHERE table_name = 'TAREA' AND column_name = 'DESCRIPCION';

    IF v_col_exists = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE TAREA ADD (DESCRIPCION VARCHAR2(500))';
    END IF;

    -- 2. Agregar columna DESCRIPCION a TAREARECURRENTE si no existe
    SELECT COUNT(*) INTO v_col_exists
    FROM user_tab_columns
    WHERE table_name = 'TAREARECURRENTE' AND column_name = 'DESCRIPCION';

    IF v_col_exists = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE TAREARECURRENTE ADD (DESCRIPCION VARCHAR2(500))';
    END IF;
END;
/

-- 3. Crear índice para optimizar búsqueda de instancias generadas por recurrencia
DECLARE
    v_idx_exists NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_idx_exists
    FROM user_indexes
    WHERE index_name = 'IDX_TAREA_RECURRENTE';

    IF v_idx_exists = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_tarea_recurrente ON TAREA(id_tarea_recurrente)';
    END IF;
END;
/

COMMIT;
