package com.plantopolis.backend.infrastructure.adapter.in.web;

import com.plantopolis.backend.domain.model.ArchivoBackup;
import com.plantopolis.backend.domain.port.in.GestionarBackupAdminUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminBackupControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GestionarBackupAdminUseCase useCase;

    @InjectMocks
    private AdminBackupController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void generarBackup_Retorna200YArchivo() throws Exception {
        ArchivoBackup mockArchivo = new ArchivoBackup("test.sql", "sql".getBytes(), LocalDateTime.now(), new ArrayList<>(), 0);
        // Usamos null porque standaloneSetup no tiene SecurityContext configurado por defecto.
        when(useCase.generarBackup(null)).thenReturn(mockArchivo);

        mockMvc.perform(get("/api/admin/backup/generar"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"test.sql\""))
                .andExpect(content().contentTypeCompatibleWith("application/sql"));
    }
}
