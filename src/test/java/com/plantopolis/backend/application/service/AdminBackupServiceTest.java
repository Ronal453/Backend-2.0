package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.ArchivoBackup;
import com.plantopolis.backend.domain.model.ScriptRespaldo;
import com.plantopolis.backend.domain.port.out.BackupRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminBackupServiceTest {

    @Mock
    private BackupRepositoryPort backupRepositoryPort;

    @InjectMocks
    private AdminBackupService service;

    @Test
    void generarBackup_RetornaArchivoValido() {
        ScriptRespaldo scriptMock = new ScriptRespaldo("SQL_DUMMY", new ArrayList<>(), 0);
        when(backupRepositoryPort.exportarDatosSql(anyString())).thenReturn(scriptMock);

        ArchivoBackup res = service.generarBackup("admin@test.com");

        assertNotNull(res);
        assertTrue(res.getNombreArchivo().startsWith("plantopolis_backup_"));
        verify(backupRepositoryPort).exportarDatosSql(anyString());
    }
}
