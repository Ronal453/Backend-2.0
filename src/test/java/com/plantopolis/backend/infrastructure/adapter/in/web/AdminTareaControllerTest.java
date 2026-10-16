package com.plantopolis.backend.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.plantopolis.backend.domain.model.ResumenTareas;
import com.plantopolis.backend.domain.model.Tarea;
import com.plantopolis.backend.domain.port.in.GestionarTareasAdminUseCase;
import com.plantopolis.backend.domain.port.in.GestionarTareasRecurrentesUseCase;
import com.plantopolis.backend.domain.port.in.NotificarTareasVencidasUseCase;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.CrearTareaRequest;
import com.plantopolis.backend.infrastructure.adapter.in.web.dto.ReasignarTareaRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminTareaController")
class AdminTareaControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock private GestionarTareasAdminUseCase adminTareasUseCase;
    @Mock private GestionarTareasRecurrentesUseCase recurrentesUseCase;
    @Mock private NotificarTareasVencidasUseCase notificarVencidasUseCase;
    @Mock private com.plantopolis.backend.domain.port.out.UsuarioRepositoryPort usuarioRepository;

    @InjectMocks
    private AdminTareaController controller;

    private final UserDetails adminUser = User.withUsername("admin@plantopolis.com")
            .password("pass").roles("ADMINISTRADOR").build();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new org.springframework.web.method.support.HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(org.springframework.core.MethodParameter parameter) {
                        return parameter.getParameterType().isAssignableFrom(UserDetails.class);
                    }
                    @Override
                    public Object resolveArgument(org.springframework.core.MethodParameter parameter,
                                                  org.springframework.web.method.support.ModelAndViewContainer mavContainer,
                                                  org.springframework.web.context.request.NativeWebRequest webRequest,
                                                  org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
                        return adminUser;
                    }
                })
                .build();
    }

    @Test
    @DisplayName("POST /api/admin/tareas con datos válidos retorna 201 Created")
    void crearTarea_valida_retorna201() throws Exception {
        CrearTareaRequest request = new CrearTareaRequest(
                "RIEGO", "ALTA", "Regar macetas", 1L, null, 10L, LocalDate.now().plusDays(2)
        );

        Tarea guardada = Tarea.builder()
                .idTarea(100L)
                .tipoTarea("RIEGO")
                .prioridad("ALTA")
                .estadoTarea("POR_HACER")
                .idZona(1L)
                .idTrabajadorAsignado(10L)
                .fechaLimite(LocalDate.now().plusDays(2))
                .build();

        when(adminTareasUseCase.crearTarea(any(Tarea.class), eq("admin@plantopolis.com"))).thenReturn(guardada);

        mockMvc.perform(post("/api/admin/tareas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idTarea").value(100L))
                .andExpect(jsonPath("$.estadoTarea").value("POR_HACER"));
    }

    @Test
    @DisplayName("POST /api/admin/tareas con campos vacíos retorna 400 Bad Request")
    void crearTarea_invalida_retorna400() throws Exception {
        CrearTareaRequest request = new CrearTareaRequest(
                "", "", null, null, null, null, null
        );

        mockMvc.perform(post("/api/admin/tareas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.campos.tipoTarea").exists());
    }

    @Test
    @DisplayName("GET /api/admin/tareas retorna 200 OK con lista")
    void listarTareas_retorna200() throws Exception {
        Tarea t = Tarea.builder().idTarea(1L).tipoTarea("PODA").prioridad("BAJA").estadoTarea("POR_HACER").build();
        when(adminTareasUseCase.listarTareasAdmin(any())).thenReturn(List.of(t));

        mockMvc.perform(get("/api/admin/tareas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idTarea").value(1L));
    }

    @Test
    @DisplayName("PATCH /api/admin/tareas/{id}/asignacion retorna 200 OK")
    void reasignarTarea_retorna200() throws Exception {
        ReasignarTareaRequest request = new ReasignarTareaRequest(25L);
        Tarea actualizada = Tarea.builder().idTarea(10L).idTrabajadorAsignado(25L).estadoTarea("POR_HACER").build();

        when(adminTareasUseCase.reasignarTarea(eq(10L), eq(25L), eq("admin@plantopolis.com"))).thenReturn(actualizada);

        mockMvc.perform(patch("/api/admin/tareas/10/asignacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idTrabajadorAsignado").value(25L));
    }

    @Test
    @DisplayName("GET /api/admin/tareas/resumen retorna 200 OK con agregados")
    void resumen_retorna200() throws Exception {
        ResumenTareas resumen = ResumenTareas.builder()
                .totalActivas(10)
                .vencidasActivas(2)
                .completadasEnPeriodo(5)
                .sinAsignar(1)
                .conteoPorEstado(Collections.emptyMap())
                .cargaPorTrabajador(Collections.emptyList())
                .completadasPorDia(Collections.emptyList())
                .tareasVencidas(Collections.emptyList())
                .build();

        when(adminTareasUseCase.obtenerResumen(any(), any())).thenReturn(resumen);

        mockMvc.perform(get("/api/admin/tareas/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActivas").value(10))
                .andExpect(jsonPath("$.vencidasActivas").value(2));
    }

    @Test
    @DisplayName("GET /api/admin/tareas/trabajadores retorna 200 OK con lista de operarios")
    void listarTrabajadores_retorna200() throws Exception {
        var operario = com.plantopolis.backend.domain.model.Usuario.builder()
                .idUsuario(4L)
                .nombreCompleto("Juan Operario")
                .correo("juan@plantopolis.com")
                .rolNombre("TRABAJADOR")
                .activo(true)
                .build();

        when(usuarioRepository.listarActivosPorRol(2L)).thenReturn(List.of(operario));

        mockMvc.perform(get("/api/admin/tareas/trabajadores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idUsuario").value(4L))
                .andExpect(jsonPath("$[0].nombreCompleto").value("Juan Operario"))
                .andExpect(jsonPath("$[0].correo").value("juan@plantopolis.com"));
    }
}
