package com.plantopolis.backend.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plantopolis.backend.domain.model.Usuario;
import com.plantopolis.backend.domain.port.in.GestionarUsuariosAdminUseCase;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.CrearTrabajadorRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUsuarioController — HU11")
class AdminUsuarioControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private GestionarUsuariosAdminUseCase usuariosUseCase;

    @InjectMocks
    private AdminUsuarioController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("POST /api/admin/usuarios/trabajadores")
    class CrearTrabajadorEndpointTests {

        @Test
        @DisplayName("201 CREATED — Crea cuenta de trabajador con datos válidos")
        void crearTrabajador_datosValidos_retorna201() throws Exception {
            var request = new CrearTrabajadorRequest(
                    "Carlos Gómez",
                    "carlos.gomez@plantopolis.com",
                    "Temporal2024"
            );

            var usuarioCreado = Usuario.builder()
                    .idUsuario(15L)
                    .nombreCompleto("Carlos Gómez")
                    .correo("carlos.gomez@plantopolis.com")
                    .idRol(2L)
                    .rolNombre("TRABAJADOR")
                    .activo(true)
                    .fechaRegistro(LocalDateTime.now())
                    .build();

            when(usuariosUseCase.crearTrabajador(
                    eq("Carlos Gómez"),
                    eq("carlos.gomez@plantopolis.com"),
                    eq("Temporal2024")
            )).thenReturn(usuarioCreado);

            mockMvc.perform(post("/api/admin/usuarios/trabajadores")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.idUsuario").value(15))
                    .andExpect(jsonPath("$.nombreCompleto").value("Carlos Gómez"))
                    .andExpect(jsonPath("$.correo").value("carlos.gomez@plantopolis.com"))
                    .andExpect(jsonPath("$.rol").value("TRABAJADOR"))
                    .andExpect(jsonPath("$.activo").value(true));

            verify(usuariosUseCase).crearTrabajador("Carlos Gómez", "carlos.gomez@plantopolis.com", "Temporal2024");
        }

        @Test
        @DisplayName("400 BAD REQUEST — Rechaza solicitud cuando el correo no tiene formato válido")
        void crearTrabajador_correoInvalido_retorna400() throws Exception {
            var request = new CrearTrabajadorRequest(
                    "Carlos Gómez",
                    "correo-invalido",
                    "Temporal2024"
            );

            mockMvc.perform(post("/api/admin/usuarios/trabajadores")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(usuariosUseCase, never()).crearTrabajador(any(), any(), any());
        }

        @Test
        @DisplayName("400 BAD REQUEST — Rechaza solicitud cuando la contraseña es menor a 6 caracteres")
        void crearTrabajador_passwordCorta_retorna400() throws Exception {
            var request = new CrearTrabajadorRequest(
                    "Carlos Gómez",
                    "carlos@plantopolis.com",
                    "123"
            );

            mockMvc.perform(post("/api/admin/usuarios/trabajadores")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(usuariosUseCase, never()).crearTrabajador(any(), any(), any());
        }

        @Test
        @DisplayName("409 CONFLICT — Retorna 409 cuando el correo ya existe en el sistema")
        void crearTrabajador_correoDuplicado_retorna409() throws Exception {
            var request = new CrearTrabajadorRequest(
                    "Carlos Gómez",
                    "carlos.duplicado@plantopolis.com",
                    "Temporal2024"
            );

            when(usuariosUseCase.crearTrabajador(any(), eq("carlos.duplicado@plantopolis.com"), any()))
                    .thenThrow(new RuntimeException("El email ya está registrado: carlos.duplicado@plantopolis.com"));

            mockMvc.perform(post("/api/admin/usuarios/trabajadores")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.mensaje").value("El email ya está registrado: carlos.duplicado@plantopolis.com"));
        }
    }
}
