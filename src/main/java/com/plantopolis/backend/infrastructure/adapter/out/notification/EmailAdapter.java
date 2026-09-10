package com.plantopolis.backend.infrastructure.adapter.out.notification;

import com.plantopolis.backend.domain.model.DetallePedido;
import com.plantopolis.backend.domain.model.Pedido;
import com.plantopolis.backend.domain.port.out.NotificacionPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Adaptador de notificaciones por correo electrónico.
 *
 * CAMBIO IMPORTANTE: se reemplazó JavaMailSender (SMTP) por la API HTTP
 * de Resend (https://resend.com), porque la red donde se despliega el
 * backend bloquea los puertos SMTP salientes (25, 465, 587) — algo común
 * en redes universitarias o de ISPs residenciales. La API de Resend usa
 * HTTPS (puerto 443), que no está bloqueado.
 *
 * La lógica de construcción de las plantillas HTML se mantiene igual;
 * solo cambia el mecanismo de envío (antes SMTP, ahora POST HTTP).
 *
 * Configuración requerida en application.yml / variables de entorno:
 *   resend.api-key=${RESEND_API_KEY}
 *   resend.mail-from=${MAIL_FROM}
 */
@Slf4j
@Component
public class EmailAdapter implements NotificacionPort {

    // Cliente HTTP reutilizable para llamar a la API de Resend
    private final RestClient restClient;

    // Dirección remitente configurada (ej: onboarding@resend.dev en pruebas,
    // o un dominio propio verificado en Resend para producción)
    @Value("${resend.mail-from}")
    private String mailFrom;

    // API Key de Resend, leída del .env / variables de entorno
    @Value("${resend.api-key}")
    private String resendApiKey;

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    private static final DateTimeFormatter FECHA_FORMATO =
            DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy HH:mm",
                    java.util.Locale.forLanguageTag("es-CO"));

    public EmailAdapter() {
        this.restClient = RestClient.create();
    }

    // ─────────────────────────────────────────────────────────────────────
    /**
     * Envía un correo genérico a través de la API HTTP de Resend.
     * Centraliza la llamada para no repetir código entre los dos métodos
     * públicos (confirmación de pedido y cambio de estado).
     *
     * @param destinatario email del cliente
     * @param asunto       asunto del correo
     * @param htmlBody     cuerpo HTML ya construido
     */
    private void enviarEmailViaResend(String destinatario, String asunto, String htmlBody) {
        // Cuerpo de la petición según la API de Resend:
        // https://resend.com/docs/api-reference/emails/send-email
        Map<String, Object> body = Map.of(
                "from", "🌱 Plantopolis <" + mailFrom + ">",
                "to", List.of(destinatario),
                "subject", asunto,
                "html", htmlBody
        );

        restClient.post()
                .uri(RESEND_API_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + resendApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    // ─────────────────────────────────────────────────────────────────────
    /**
     * Envía el email de confirmación cuando se crea un pedido exitosamente.
     * Misma plantilla HTML de siempre; solo cambia el transporte (HTTP en
     * vez de SMTP).
     *
     * @param pedido dominio con toda la información del pedido recién creado
     */
    @Override
    public void enviarConfirmacionPedido(Pedido pedido) {
        try {
            log.info("Enviando email de confirmación para pedido: {} a: {}",
                    pedido.getNumeroPedido(), pedido.getEmailCliente());

            String asunto = "✅ Pedido " + pedido.getNumeroPedido()
                    + " confirmado — Plantopolis";
            String html = construirHtmlConfirmacion(pedido);

            enviarEmailViaResend(pedido.getEmailCliente(), asunto, html);

            log.info("✅ Email de confirmación enviado exitosamente a: {}",
                    pedido.getEmailCliente());

        } catch (Exception e) {
            // Se loguea pero NO se relanza, para no cancelar la transacción
            // del pedido si el email falla por cualquier motivo.
            log.error("❌ Error enviando email de confirmación (pedido: {}): {}",
                    pedido.getNumeroPedido(), e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    /**
     * Envía el email de notificación cuando el admin cambia el estado del pedido.
     *
     * @param pedido dominio con el nuevo estado actualizado
     */
    @Override
    public void enviarCambioDeEstado(Pedido pedido) {
        try {
            log.info("Enviando email de cambio de estado '{}' para pedido: {} a: {}",
                    pedido.getEstadoDescripcion(),
                    pedido.getNumeroPedido(),
                    pedido.getEmailCliente());

            String asunto = obtenerAsuntoPorEstado(pedido.getEstadoDescripcion())
                    + " — Pedido " + pedido.getNumeroPedido();
            String html = construirHtmlCambioEstado(pedido);

            enviarEmailViaResend(pedido.getEmailCliente(), asunto, html);

            log.info("✅ Email de cambio de estado '{}' enviado a: {}",
                    pedido.getEstadoDescripcion(), pedido.getEmailCliente());

        } catch (Exception e) {
            log.error("❌ Error enviando email de cambio de estado (pedido: {}): {}",
                    pedido.getNumeroPedido(), e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // PLANTILLAS HTML (sin cambios respecto a la versión SMTP)
    // ═════════════════════════════════════════════════════════════════════

    private String construirHtmlConfirmacion(Pedido pedido) {

        String fechaFormateada;
        if (pedido.getFechaPedido() != null) {
            ZonedDateTime colombiaTime = pedido.getFechaPedido()
                .atZone(ZoneOffset.UTC)
                .withZoneSameInstant(ZoneId.of("America/Bogota"));
            fechaFormateada = colombiaTime.format(FECHA_FORMATO);
        } else {
            fechaFormateada = "Fecha no disponible";
        }

        StringBuilder html = new StringBuilder();

        html.append("""
            <!DOCTYPE html>
            <html lang="es">
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
              <title>Confirmación de Pedido — Plantopolis</title>
            </head>
            <body style="margin:0;padding:0;background-color:#f4f7f4;
                         font-family:Arial,Helvetica,sans-serif;">
            
              <table width="100%%" cellpadding="0" cellspacing="0"
                     style="background-color:#f4f7f4;padding:20px 0;">
                <tr>
                  <td align="center">
                    <table width="600" cellpadding="0" cellspacing="0"
                           style="max-width:600px;width:100%%;">
            """);

        html.append("""
              <tr>
                <td style="background-color:#0E4F2F;border-radius:12px 12px 0 0;
                           padding:32px 40px;text-align:center;">
                  <p style="margin:0;font-size:36px;">🌱</p>
                  <h1 style="margin:8px 0 0;color:#ffffff;font-size:26px;
                             font-weight:700;letter-spacing:-0.5px;">
                    Plantopolis
                  </h1>
                  <p style="margin:6px 0 0;color:#a8d5b8;font-size:13px;">
                    Tu tienda de plantas online
                  </p>
                </td>
              </tr>
            """);

        html.append("""
              <tr>
                <td style="background-color:#ffffff;padding:40px;">
            """);

        html.append(String.format("""
              <div style="text-align:center;margin-bottom:28px;">
                <div style="background-color:#e8f5e9;border-radius:50%%;
                            width:64px;height:64px;margin:0 auto 16px;
                            display:inline-flex;align-items:center;
                            justify-content:center;font-size:32px;">
                  ✅
                </div>
                <h2 style="margin:0;color:#0E4F2F;font-size:22px;font-weight:700;">
                  ¡Pedido confirmado!
                </h2>
                <p style="margin:8px 0 0;color:#555555;font-size:15px;">
                  Hola <strong>%s</strong>, recibimos tu pedido correctamente.
                </p>
              </div>
            """, pedido.getNombreCliente()));

        html.append(String.format("""
              <div style="background-color:#f0faf4;border:2px solid #0E4F2F;
                          border-radius:10px;padding:18px 24px;margin-bottom:28px;
                          text-align:center;">
                <p style="margin:0;font-size:12px;color:#666666;
                          text-transform:uppercase;letter-spacing:1px;">
                  Número de pedido
                </p>
                <p style="margin:6px 0 0;font-size:22px;font-weight:700;
                          color:#0E4F2F;letter-spacing:1px;">
                  %s
                </p>
                <p style="margin:6px 0 0;font-size:13px;color:#888888;">
                  Fecha: %s
                </p>
              </div>
            """, pedido.getNumeroPedido(), fechaFormateada));

        html.append("""
              <h3 style="margin:0 0 14px;color:#1a1a1a;font-size:16px;
                         font-weight:700;border-bottom:2px solid #e8f5e9;
                         padding-bottom:10px;">
                🛒 Resumen de tu pedido
              </h3>
            
              <table width="100%%" cellpadding="0" cellspacing="0"
                     style="border-collapse:collapse;margin-bottom:20px;">
                <thead>
                  <tr style="background-color:#0E4F2F;">
                    <th style="padding:12px 16px;color:#ffffff;font-size:13px;
                               text-align:left;border-radius:6px 0 0 0;">
                      Producto
                    </th>
                    <th style="padding:12px 8px;color:#ffffff;font-size:13px;
                               text-align:center;">
                      Cant.
                    </th>
                    <th style="padding:12px 8px;color:#ffffff;font-size:13px;
                               text-align:right;">
                      P. Unitario
                    </th>
                    <th style="padding:12px 16px;color:#ffffff;font-size:13px;
                               text-align:right;border-radius:0 6px 0 0;">
                      Subtotal
                    </th>
                  </tr>
                </thead>
                <tbody>
            """);

        List<DetallePedido> detalles = pedido.getDetalles();
        if (detalles != null) {
            for (int i = 0; i < detalles.size(); i++) {
                DetallePedido detalle = detalles.get(i);
                String bgColor = (i % 2 == 0) ? "#ffffff" : "#f9fdf9";

                BigDecimal subtotal = detalle.getSubtotal() != null
                        ? detalle.getSubtotal()
                        : detalle.getPrecioUnitario()
                                .multiply(BigDecimal.valueOf(detalle.getCantidad()));

                html.append(String.format("""
                    <tr style="background-color:%s;">
                      <td style="padding:14px 16px;border-bottom:1px solid #f0f0f0;
                                 font-size:14px;color:#333333;">
                        <span style="font-weight:600;">%s</span>
                      </td>
                      <td style="padding:14px 8px;border-bottom:1px solid #f0f0f0;
                                 font-size:14px;color:#555555;text-align:center;">
                        %d
                      </td>
                      <td style="padding:14px 8px;border-bottom:1px solid #f0f0f0;
                                 font-size:14px;color:#555555;text-align:right;">
                        $%s
                      </td>
                      <td style="padding:14px 16px;border-bottom:1px solid #f0f0f0;
                                 font-size:14px;font-weight:600;color:#0E4F2F;
                                 text-align:right;">
                        $%s
                      </td>
                    </tr>
                    """,
                        bgColor,
                        detalle.getNombreProducto() != null
                                ? detalle.getNombreProducto() : "Producto",
                        detalle.getCantidad(),
                        formatearPrecio(detalle.getPrecioUnitario()),
                        formatearPrecio(subtotal)));
            }
        }

        html.append("</tbody></table>");

        html.append(String.format("""
              <div style="background-color:#0E4F2F;border-radius:8px;
                          padding:16px 24px;margin-bottom:24px;
                          display:flex;justify-content:space-between;
                          align-items:center;">
                <table width="100%%" cellpadding="0" cellspacing="0">
                  <tr>
                    <td style="color:#a8d5b8;font-size:14px;">Total pagado</td>
                    <td style="text-align:right;color:#ffffff;font-size:22px;
                               font-weight:700;">
                      $%s COP
                    </td>
                  </tr>
                </table>
              </div>
            """, formatearPrecio(pedido.getTotal())));

        html.append("""
              <table width="100%%" cellpadding="0" cellspacing="0"
                     style="margin-bottom:28px;">
                <tr>
            """);

        String metodoPago = (pedido.getPago() != null
                && pedido.getPago().getNombreMetodo() != null)
                ? pedido.getPago().getNombreMetodo().replace("_", " ")
                : "No especificado";

        String estadoPagoEmail = (pedido.getPago() != null
                && "PENDIENTE".equals(pedido.getPago().getEstadoPagoDescripcion()))
                ? "PENDIENTE" : "APROBADO";

        String colorEstadoPago = "PENDIENTE".equals(estadoPagoEmail)
                ? "#e65100" : "#2e7d32";
        String iconoEstadoPago = "PENDIENTE".equals(estadoPagoEmail)
                ? "⏳" : "✅";

        html.append(String.format("""
                  <td style="width:48%%;vertical-align:top;">
                    <div style="background-color:#f8f9fa;border-radius:8px;
                                padding:16px 20px;border-left:4px solid #0E4F2F;">
                      <p style="margin:0 0 6px;font-size:11px;color:#888888;
                                text-transform:uppercase;letter-spacing:0.8px;">
                        💳 Método de pago
                      </p>
                      <p style="margin:0;font-size:14px;font-weight:600;
                                color:#1a1a1a;">
                        %s
                      </p>
                      <p style="margin:4px 0 0;font-size:12px;font-weight:600;
                                color:%s;">
                        %s %s
                      </p>
                    </div>
                  </td>
                  <td style="width:4%%;"></td>
            """, metodoPago, colorEstadoPago, iconoEstadoPago, estadoPagoEmail));

        html.append(String.format("""
                  <td style="width:48%%;vertical-align:top;">
                    <div style="background-color:#f8f9fa;border-radius:8px;
                                padding:16px 20px;border-left:4px solid #4caf50;">
                      <p style="margin:0 0 6px;font-size:11px;color:#888888;
                                text-transform:uppercase;letter-spacing:0.8px;">
                        📦 Dirección de envío
                      </p>
                      <p style="margin:0;font-size:14px;color:#333333;
                                line-height:1.5;">
                        %s
                      </p>
                    </div>
                  </td>
            """, pedido.getDireccionEnvio() != null
                ? pedido.getDireccionEnvio() : "No especificada"));

        html.append("</tr></table>");

        html.append("""
              <div style="background-color:#fff8e1;border-radius:8px;
                          padding:16px 20px;border-left:4px solid #ffc107;
                          margin-bottom:20px;">
                <p style="margin:0;font-size:14px;color:#7c5a00;line-height:1.6;">
                  📬 <strong>¿Qué sigue?</strong> Recibirás actualizaciones
                  sobre el estado de tu pedido por este mismo correo.
                  Puedes ver el estado en tiempo real en la sección
                  <strong>"Mis pedidos"</strong> de Plantopolis.
                </p>
              </div>
            """);

        html.append("</td></tr>");

        html.append("""
              <tr>
                <td style="background-color:#1a1a1a;border-radius:0 0 12px 12px;
                           padding:24px 40px;text-align:center;">
                  <p style="margin:0 0 8px;font-size:16px;">🌱</p>
                  <p style="margin:0 0 6px;color:#ffffff;font-size:14px;
                             font-weight:600;">
                    Plantopolis
                  </p>
                  <p style="margin:0 0 12px;color:#888888;font-size:12px;">
                    Tu tienda de plantas online
                  </p>
                  <p style="margin:0;color:#555555;font-size:11px;line-height:1.6;">
                    Si tienes preguntas sobre tu pedido, responde este correo.<br/>
                    Este es un correo automático — no respondas directamente
                    si ves una dirección no monitoreada.
                  </p>
                </td>
              </tr>
            """);

        html.append("""
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """);

        return html.toString();
    }

    private String construirHtmlCambioEstado(Pedido pedido) {

        String estado = pedido.getEstadoDescripcion() != null
                ? pedido.getEstadoDescripcion() : "ACTUALIZADO";

        String emoji      = obtenerEmojiPorEstado(estado);
        String colorBadge = obtenerColorPorEstado(estado);
        String mensajeEstado = obtenerMensajePorEstado(estado);
        String descripcionEstado = obtenerDescripcionPorEstado(estado);

        String fechaPedido = pedido.getFechaPedido() != null
                ? pedido.getFechaPedido().format(FECHA_FORMATO)
                : "No disponible";

        return String.format("""
            <!DOCTYPE html>
            <html lang="es">
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
              <title>Actualización de Pedido — Plantopolis</title>
            </head>
            <body style="margin:0;padding:0;background-color:#f4f7f4;
                         font-family:Arial,Helvetica,sans-serif;">
            
              <table width="100%%" cellpadding="0" cellspacing="0"
                     style="background-color:#f4f7f4;padding:20px 0;">
                <tr>
                  <td align="center">
                    <table width="600" cellpadding="0" cellspacing="0"
                           style="max-width:600px;width:100%%;">
            
                      <tr>
                        <td style="background-color:#0E4F2F;
                                   border-radius:12px 12px 0 0;
                                   padding:32px 40px;text-align:center;">
                          <p style="margin:0;font-size:36px;">🌱</p>
                          <h1 style="margin:8px 0 0;color:#ffffff;font-size:26px;
                                     font-weight:700;">
                            Plantopolis
                          </h1>
                          <p style="margin:6px 0 0;color:#a8d5b8;font-size:13px;">
                            Actualización de tu pedido
                          </p>
                        </td>
                      </tr>
            
                      <tr>
                        <td style="background-color:#ffffff;padding:40px;">
            
                          <div style="text-align:center;margin-bottom:28px;">
                            <div style="font-size:52px;margin-bottom:12px;">
                              %s
                            </div>
                            <h2 style="margin:0;color:#1a1a1a;font-size:22px;
                                       font-weight:700;">
                              %s
                            </h2>
                            <p style="margin:8px 0 0;color:#555555;font-size:15px;">
                              Hola <strong>%s</strong>, te informamos sobre
                              la actualización de tu pedido.
                            </p>
                          </div>
            
                          <div style="background-color:#f8f9fa;border-radius:10px;
                                      padding:18px 24px;margin-bottom:24px;
                                      text-align:center;">
                            <p style="margin:0;font-size:12px;color:#888888;
                                      text-transform:uppercase;letter-spacing:1px;">
                              Número de pedido
                            </p>
                            <p style="margin:6px 0 0;font-size:20px;font-weight:700;
                                      color:#0E4F2F;">
                              %s
                            </p>
                            <p style="margin:4px 0 0;font-size:12px;color:#888888;">
                              Pedido realizado el %s
                            </p>
                          </div>
            
                          <div style="text-align:center;margin-bottom:28px;">
                            <p style="margin:0 0 10px;font-size:14px;color:#666666;">
                              Estado actual de tu pedido:
                            </p>
                            <span style="background-color:%s;color:#ffffff;
                                         font-size:16px;font-weight:700;
                                         padding:10px 28px;border-radius:25px;
                                         letter-spacing:0.5px;">
                              %s  %s
                            </span>
                          </div>
            
                          <div style="background-color:#f0faf4;border-radius:8px;
                                      border-left:4px solid #0E4F2F;
                                      padding:20px 24px;margin-bottom:24px;">
                            <p style="margin:0;font-size:15px;color:#333333;
                                      line-height:1.7;">
                              %s
                            </p>
                          </div>
            
                          <h3 style="margin:0 0 16px;color:#1a1a1a;font-size:15px;
                                     font-weight:700;">
                            📍 Seguimiento del pedido
                          </h3>
            
                          <table width="100%%" cellpadding="0" cellspacing="0"
                                 style="margin-bottom:28px;">
                            %s
                          </table>
            
                          <div style="background-color:#f8f9fa;border-radius:8px;
                                      padding:16px 20px;text-align:center;">
                            <p style="margin:0;font-size:13px;color:#666666;
                                      line-height:1.6;">
                              Puedes ver el detalle completo de tu pedido en
                              <strong>Mis pedidos</strong> dentro de Plantopolis.
                              ¿Tienes preguntas? Responde este correo.
                            </p>
                          </div>
            
                        </td>
                      </tr>
            
                      <tr>
                        <td style="background-color:#1a1a1a;
                                   border-radius:0 0 12px 12px;
                                   padding:24px 40px;text-align:center;">
                          <p style="margin:0 0 6px;color:#ffffff;font-size:14px;
                                     font-weight:600;">
                            🌱 Plantopolis
                          </p>
                          <p style="margin:0;color:#555555;font-size:11px;">
                            Tu tienda de plantas online
                          </p>
                        </td>
                      </tr>
            
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """,
                emoji, mensajeEstado, pedido.getNombreCliente(),
                pedido.getNumeroPedido(), fechaPedido, colorBadge,
                emoji, estado, descripcionEstado,
                construirLineaDeTiempoHtml(estado)
        );
    }

    // ═════════════════════════════════════════════════════════════════════
    // MÉTODOS AUXILIARES (sin cambios)
    // ═════════════════════════════════════════════════════════════════════

    private String construirLineaDeTiempoHtml(String estadoActual) {
        String[][] pasos = {
            {"PENDIENTE",  "⏳", "Pedido recibido",
             "Tu pedido fue recibido y está en espera de procesamiento."},
            {"EN_PREPARACION", "🌿", "En preparación",
             "Estamos seleccionando y empacando tus plantas con cuidado."},
            {"ENVIADO",    "🚚", "En camino",
             "Tu pedido salió de nuestras instalaciones hacia tu dirección."},
            {"ENTREGADO",  "✅", "Entregado",
             "Tu pedido fue entregado exitosamente."}
        };

        if ("CANCELADO".equals(estadoActual)) {
            return """
                <tr>
                  <td>
                    <div style="background-color:#ffebee;border-radius:8px;
                                border-left:4px solid #f44336;padding:16px 20px;
                                text-align:center;">
                      <p style="margin:0;font-size:32px;">❌</p>
                      <p style="margin:8px 0 0;font-size:16px;font-weight:700;
                                color:#c62828;">
                        Pedido Cancelado
                      </p>
                      <p style="margin:6px 0 0;font-size:13px;color:#e53935;">
                        Tu pedido ha sido cancelado. Si tienes dudas,
                        contáctanos respondiendo este correo.
                      </p>
                    </div>
                  </td>
                </tr>
                """;
        }

        int indiceActual = 0;
        for (int i = 0; i < pasos.length; i++) {
            if (pasos[i][0].equals(estadoActual)) {
                indiceActual = i;
                break;
            }
        }

        StringBuilder linea = new StringBuilder();

        for (int i = 0; i < pasos.length; i++) {
            boolean completado = i < indiceActual;
            boolean actual     = i == indiceActual;
            boolean pendiente  = i > indiceActual;

            String bgColor    = actual    ? "#e8f5e9"
                              : completado ? "#f1f8e9"
                              : "#f8f8f8";
            String borderColor = actual    ? "#0E4F2F"
                               : completado ? "#4caf50"
                               : "#e0e0e0";
            String textColor   = pendiente ? "#bdbdbd" : "#333333";
            String emojiMostrar = completado ? "✅" : pasos[i][1];

            linea.append(String.format("""
                <tr>
                  <td style="padding:0 0 8px 0;">
                    <div style="background-color:%s;border-radius:8px;
                                border-left:4px solid %s;padding:12px 16px;
                                display:flex;align-items:center;">
                      <table cellpadding="0" cellspacing="0" width="100%%">
                        <tr>
                          <td style="width:32px;font-size:20px;vertical-align:middle;">
                            %s
                          </td>
                          <td style="padding-left:12px;vertical-align:middle;">
                            <p style="margin:0;font-size:14px;font-weight:%s;
                                      color:%s;">
                              %s
                            </p>
                            <p style="margin:2px 0 0;font-size:12px;color:#888888;">
                              %s
                            </p>
                          </td>
                          %s
                        </tr>
                      </table>
                    </div>
                  </td>
                </tr>
                """,
                    bgColor, borderColor,
                    emojiMostrar,
                    actual ? "700" : "400",
                    textColor,
                    pasos[i][2],
                    actual ? pasos[i][3] : "",
                    actual ? "<td style='text-align:right;padding-left:8px;" +
                             "white-space:nowrap;'><span style='background-color:#0E4F2F;" +
                             "color:#fff;font-size:10px;font-weight:700;padding:3px 10px;" +
                             "border-radius:12px;'>ACTUAL</span></td>" : "<td></td>"
            ));
        }

        return linea.toString();
    }

    private String obtenerEmojiPorEstado(String estado) {
        return switch (estado) {
            case "EN_PREPARACION" -> "🌿";
            case "ENVIADO"    -> "🚚";
            case "ENTREGADO"  -> "🎉";
            case "CANCELADO"  -> "❌";
            default           -> "📦";
        };
    }

    private String obtenerColorPorEstado(String estado) {
        return switch (estado) {
            case "EN_PREPARACION" -> "#1565C0";
            case "ENVIADO"    -> "#6A1B9A";
            case "ENTREGADO"  -> "#2E7D32";
            case "CANCELADO"  -> "#C62828";
            default           -> "#E65100";
        };
    }

    private String obtenerMensajePorEstado(String estado) {
        return switch (estado) {
            case "EN_PREPARACION" -> "Estamos preparando tu pedido";
            case "ENVIADO"    -> "¡Tu pedido está en camino!";
            case "ENTREGADO"  -> "¡Tu pedido fue entregado!";
            case "CANCELADO"  -> "Tu pedido fue cancelado";
            default           -> "Actualización de tu pedido";
        };
    }

    private String obtenerDescripcionPorEstado(String estado) {
        return switch (estado) {
            case "EN_PREPARACION" ->
                "Nuestro equipo está seleccionando y empacando tus plantas " +
                "con el mayor cuidado. Te notificaremos cuando tu pedido " +
                "sea enviado. ⏱ Tiempo estimado: 1-2 días hábiles.";
            case "ENVIADO" ->
                "Tu pedido salió de nuestras instalaciones y está en camino " +
                "hacia tu dirección. El transportador realizará la entrega " +
                "en los próximos días hábiles. 📍 Mantente pendiente.";
            case "ENTREGADO" ->
                "¡Felicitaciones! Tu pedido fue entregado exitosamente. " +
                "Esperamos que tus plantas lleguen perfectas y llenen de " +
                "vida tu hogar. 🌱 ¡Gracias por comprar en Plantopolis!";
            case "CANCELADO" ->
                "Lamentamos informarte que tu pedido ha sido cancelado. " +
                "Si realizaste un pago, será procesado el reembolso en " +
                "los próximos días hábiles. Para más información, " +
                "responde este correo.";
            default ->
                "El estado de tu pedido ha sido actualizado. " +
                "Puedes ver los detalles completos en la sección " +
                "'Mis pedidos' de Plantopolis.";
        };
    }

    private String obtenerAsuntoPorEstado(String estado) {
        return switch (estado) {
            case "EN_PREPARACION" -> "🌿 Estamos preparando tu pedido";
            case "ENVIADO"    -> "🚚 Tu pedido está en camino";
            case "ENTREGADO"  -> "🎉 ¡Tu pedido fue entregado!";
            case "CANCELADO"  -> "❌ Tu pedido fue cancelado";
            default           -> "📦 Actualización de tu pedido";
        };
    }

    private String formatearPrecio(BigDecimal precio) {
        if (precio == null) return "0";
        return String.format("%,.0f", precio);
    }
}