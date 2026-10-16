package com.plantopolis.backend.application.service;

import com.plantopolis.backend.domain.model.Tarea;
import com.plantopolis.backend.domain.model.Usuario;
import com.plantopolis.backend.domain.port.in.NotificarTareasVencidasUseCase;
import com.plantopolis.backend.domain.port.out.NotificacionPort;
import com.plantopolis.backend.domain.port.out.TareaRepositoryPort;
import com.plantopolis.backend.domain.port.out.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Servicio de aplicación para detectar tareas vencidas y notificar a los administradores por email (HU28).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionTareasService implements NotificarTareasVencidasUseCase {

    private final TareaRepositoryPort tareaRepo;
    private final UsuarioRepositoryPort usuarioRepo;
    private final NotificacionPort notificacionPort;

    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");
    private static final Long ROL_ADMINISTRADOR = 1L;

    @Override
    public int notificarTareasVencidas(LocalDate hoy) {
        LocalDate fechaCorte = (hoy != null) ? hoy : LocalDate.now(ZONA_BOGOTA);
        log.info("Iniciando detección de tareas vencidas sin alerta para la fecha: {}", fechaCorte);

        List<Tarea> vencidas = tareaRepo.buscarVencidasSinAlerta(fechaCorte);
        if (vencidas.isEmpty()) {
            log.info("No hay tareas vencidas pendientes de notificación.");
            return 0;
        }

        List<Usuario> administradores = usuarioRepo.listarActivosPorRol(ROL_ADMINISTRADOR);
        if (administradores.isEmpty()) {
            log.warn("Se detectaron {} tareas vencidas, pero no hay administradores activos para notificar.", vencidas.size());
            return 0;
        }

        boolean alMenosUnEnvioExitoso = false;
        for (Usuario admin : administradores) {
            try {
                boolean enviado = notificacionPort.enviarAlertaTareasVencidas(
                        admin.getCorreo(),
                        admin.getNombreCompleto(),
                        vencidas
                );
                if (enviado) {
                    alMenosUnEnvioExitoso = true;
                }
            } catch (Exception e) {
                log.error("Fallo al enviar correo de alerta de tareas vencidas a {}: {}", admin.getCorreo(), e.getMessage());
            }
        }

        // Si se entregó exitosamente al menos un correo, marcamos la alerta como enviada para evitar duplicados
        if (alMenosUnEnvioExitoso) {
            List<Long> ids = vencidas.stream().map(Tarea::getIdTarea).toList();
            tareaRepo.marcarAlertaVencidaEnviada(ids);
            log.info("Se notificaron exitosamente {} tareas vencidas a los administradores.", vencidas.size());
            return vencidas.size();
        } else {
            log.warn("No se pudo entregar la alerta de tareas vencidas a ningún administrador. Se conservan pendientes.");
            return 0;
        }
    }
}
