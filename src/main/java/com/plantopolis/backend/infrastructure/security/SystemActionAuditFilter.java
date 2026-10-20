package com.plantopolis.backend.infrastructure.security;

import com.plantopolis.backend.domain.model.LogSistema;
import com.plantopolis.backend.domain.port.in.ConsultarLogsSistemaUseCase;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Filtro que registra en tiempo real las acciones realizadas en el sistema
 * traduciéndolas a mensajes sencillos y claros para el panel de administración.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemActionAuditFilter extends OncePerRequestFilter {

    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");

    private final ConsultarLogsSistemaUseCase logsSistemaUseCase;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        long inicio = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            registrarAccionSiCorresponde(request, response, System.currentTimeMillis() - inicio);
        }
    }

    /**
     * Evalúa si la petición corresponde a una acción relevante del sistema y genera
     * un mensaje amigable indicando qué se hizo y quién lo realizó.
     */
    private void registrarAccionSiCorresponde(HttpServletRequest request, HttpServletResponse response, long duracionMs) {
        try {
            String uri = request.getRequestURI();
            String metodo = request.getMethod();
            int status = response.getStatus();

            if (uri == null || !uri.startsWith("/api/")) {
                return;
            }
            if (uri.startsWith("/api/admin/logs")) {
                return;
            }

            boolean esMutacion = "POST".equalsIgnoreCase(metodo)
                    || "PUT".equalsIgnoreCase(metodo)
                    || "PATCH".equalsIgnoreCase(metodo)
                    || "DELETE".equalsIgnoreCase(metodo);
            boolean esAccionLecturaImportante = uri.contains("/backup/generar") || uri.contains("/exportar-csv");

            if (!esMutacion && !esAccionLecturaImportante) {
                return;
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String usuario = extraerUsuario(auth);
            String rol = extraerRol(auth, uri);

            String modulo = resolverModulo(uri);
            String accion = resolverTituloAmigable(metodo, uri, status);
            String descripcion = construirDescripcionAmigable(metodo, uri, status, usuario);
            String nivel = resolverNivel(status, metodo, uri);

            logsSistemaUseCase.registrarEvento(LogSistema.builder()
                    .fechaHora(LocalDateTime.now(ZONA_BOGOTA))
                    .nivel(nivel)
                    .modulo(modulo)
                    .accion(accion)
                    .descripcion(descripcion)
                    .usuario(usuario)
                    .rol(rol)
                    .metodoHttp(metodo)
                    .endpoint(uri)
                    .codigoEstado(status)
                    .duracionMs(duracionMs)
                    .origen("API_HTTP")
                    .build());
        } catch (Exception ex) {
            log.debug("No se pudo auditar acción: {}", ex.getMessage());
        }
    }

    private String extraerUsuario(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "Usuario visitante";
    }

    private String extraerRol(Authentication auth, String uri) {
        if (auth != null && auth.getAuthorities() != null) {
            for (GrantedAuthority authority : auth.getAuthorities()) {
                String a = authority.getAuthority();
                if (a != null && a.startsWith("ROLE_")) {
                    return a.replace("ROLE_", "");
                }
            }
        }
        if (uri.startsWith("/api/auth/")) return "ACCESO";
        return "CLIENTE";
    }

    private String resolverModulo(String uri) {
        String u = uri.toLowerCase();
        if (u.contains("/auth/") || u.contains("/usuarios")) return "USUARIOS";
        if (u.contains("/backup")) return "BACKUP";
        if (u.contains("/productos")) return "PRODUCTOS";
        if (u.contains("/pedidos") || u.contains("/reportes") || u.contains("/carrito")) return "PEDIDOS";
        if (u.contains("/tareas")) return "TAREAS";
        if (u.contains("/lotes") || u.contains("/zonas") || u.contains("/proveedores")) return "LOTES";
        if (u.contains("/mermas")) return "MERMAS";
        return "SISTEMA";
    }

    private String resolverNivel(int status, String metodo, String uri) {
        if (status >= 500) return "ERROR";
        if (status >= 400 || uri.contains("/desactivar") || uri.contains("/pausar") || "DELETE".equalsIgnoreCase(metodo)) {
            return "WARN";
        }
        return "SUCCESS";
    }

    /**
     * Genera un título corto y natural en español para la acción realizada.
     */
    private String resolverTituloAmigable(String metodo, String uri, int status) {
        if (status >= 400) return "Acción no completada";

        String u = uri.toLowerCase();
        if (u.contains("/auth/login") || u.contains("/auth/google")) return "Inicio de sesión";
        if (u.contains("/auth/registro")) return "Registro de nuevo cliente";
        if (u.contains("/auth/logout")) return "Cierre de sesión";
        if (u.contains("/backup/generar")) return "Descarga de copia de seguridad";
        if (u.contains("/backup/restaurar")) return "Restauración del sistema";
        if (u.contains("/reportes/exportar-csv")) return "Descarga de reporte de ventas";
        if (u.contains("/usuarios/trabajadores")) return "Creación de cuenta de trabajador";
        if (u.contains("/resetear-password")) return "Restablecimiento de contraseña";
        if (u.contains("/usuarios") && u.contains("/activar")) return "Activación de usuario";
        if (u.contains("/usuarios") && u.contains("/desactivar")) return "Desactivación de usuario";
        if (u.contains("/productos") && u.contains("/activar")) return "Producto habilitado en catálogo";
        if (u.contains("/productos") && u.contains("/desactivar")) return "Producto ocultado del catálogo";
        if (u.contains("/productos") && "POST".equalsIgnoreCase(metodo)) return "Nuevo producto agregado";
        if (u.contains("/productos") && "PUT".equalsIgnoreCase(metodo)) return "Producto actualizado";
        if (u.contains("/pedidos") && u.contains("/estado")) return "Actualización de estado de pedido";
        if (u.contains("/pedidos")) return "Nuevo pedido realizado";
        if (u.contains("/carrito")) return "Actualización de carrito de compras";
        if (u.contains("/reasignar")) return "Reasignación de tarea";
        if (u.contains("/procesar-alertas-vencidas")) return "Envío de recordatorios de tareas vencidas";
        if (u.contains("/generar-pendientes")) return "Generación de tareas del día";
        if (u.contains("/tareas") && "POST".equalsIgnoreCase(metodo)) return "Nueva tarea creada";
        if (u.contains("/tareas")) return "Actualización de tarea";
        if (u.contains("/vincular")) return "Lote enviado a la venta";
        if (u.contains("/lotes") && "POST".equalsIgnoreCase(metodo)) return "Nuevo lote sembrado";
        if (u.contains("/lotes")) return "Actualización de lote de cultivo";
        if (u.contains("/mermas")) return "Registro de pérdida (merma)";
        if (u.contains("/zonas")) return "Actualización de zona del vivero";
        if (u.contains("/proveedores")) return "Actualización de proveedores";

        return "Acción registrada en el sistema";
    }

    /**
     * Construye una explicación sencilla de la acción sin tecnicismos.
     */
    private String construirDescripcionAmigable(String metodo, String uri, int status, String usuario) {
        String u = uri.toLowerCase();
        if (status >= 400) {
            if (u.contains("/auth/login")) {
                return "Se intentó iniciar sesión con credenciales incorrectas o cuenta inactiva.";
            }
            return "El sistema rechazó la operación solicitada porque algún dato no cumplía las reglas.";
        }

        if (u.contains("/auth/login") || u.contains("/auth/google")) {
            return "El usuario ingresó correctamente a la plataforma.";
        }
        if (u.contains("/auth/registro")) {
            return "Una nueva persona creó su cuenta de cliente en la tienda.";
        }
        if (u.contains("/auth/logout")) {
            return "El usuario cerró su sesión de forma segura.";
        }
        if (u.contains("/backup/generar")) {
            return "Se descargó una copia de respaldo con toda la información actual del sistema.";
        }
        if (u.contains("/backup/restaurar")) {
            return "Se restauraron los datos del sistema a partir de un archivo de respaldo.";
        }
        if (u.contains("/reportes/exportar-csv")) {
            return "Se descargó el archivo de ventas en formato Excel/CSV.";
        }
        if (u.contains("/usuarios/trabajadores")) {
            return "El administrador registró un nuevo trabajador para el vivero.";
        }
        if (u.contains("/resetear-password")) {
            return "Se generó una clave temporal de acceso para un usuario.";
        }
        if (u.contains("/usuarios") && u.contains("/activar")) {
            return "Se volvió a habilitar el acceso a la cuenta de un usuario.";
        }
        if (u.contains("/usuarios") && u.contains("/desactivar")) {
            return "Se suspendió temporalmente el acceso de un usuario al sistema.";
        }
        if (u.contains("/productos") && "POST".equalsIgnoreCase(metodo)) {
            return "Se registró una nueva planta o producto en el inventario.";
        }
        if (u.contains("/productos") && "PUT".equalsIgnoreCase(metodo)) {
            return "Se modificó la información, precio o stock de un producto.";
        }
        if (u.contains("/productos") && u.contains("/activar")) {
            return "El producto volvió a estar visible para los clientes en la tienda.";
        }
        if (u.contains("/productos") && u.contains("/desactivar")) {
            return "El producto se desactivó para que no aparezca en el catálogo público.";
        }
        if (u.contains("/pedidos") && u.contains("/estado")) {
            return "Se actualizó el estado de entrega de un pedido de cliente.";
        }
        if (u.contains("/procesar-alertas-vencidas")) {
            return "El sistema revisó las tareas atrasadas y envió alertas por correo.";
        }
        if (u.contains("/generar-pendientes")) {
            return "El sistema programó automáticamente las tareas repetitivas de hoy.";
        }
        if (u.contains("/vincular")) {
            return "Las plantas de un lote listo se sumaron al stock disponible para la venta.";
        }

        return "Operación realizada exitosamente por " + usuario + ".";
    }
}
