package com.plantopolis.backend.domain.port.out;
import com.plantopolis.backend.domain.model.ResultadoRestauracionDatos;
import com.plantopolis.backend.domain.model.ScriptRespaldo;
import com.plantopolis.backend.domain.model.SentenciaRestauracion;
import com.plantopolis.backend.domain.model.TablaRespaldo;
import java.util.List;
public interface BackupRepositoryPort {
    List<String> listarTablasEnOrdenDependencia();
    List<TablaRespaldo> contarFilasPorTabla();
    ScriptRespaldo exportarDatosSql(String encabezado);
    ResultadoRestauracionDatos restaurarDatos(List<SentenciaRestauracion> sentencias);
}
