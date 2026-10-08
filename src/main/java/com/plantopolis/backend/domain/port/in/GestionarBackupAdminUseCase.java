package com.plantopolis.backend.domain.port.in;
import com.plantopolis.backend.domain.model.ArchivoBackup;
import com.plantopolis.backend.domain.model.ResultadoRestauracion;
import com.plantopolis.backend.domain.model.TablaRespaldo;
import java.util.List;
public interface GestionarBackupAdminUseCase {
    ArchivoBackup generarBackup(String correoAdmin);
    ResultadoRestauracion restaurarBackup(String nombreArchivo, byte[] contenido, String correoAdmin);
    List<TablaRespaldo> obtenerResumenActual();
}
