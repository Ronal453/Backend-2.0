package com.plantopolis.backend.infrastructure.adapter.out.notification;

import com.plantopolis.backend.domain.model.DetallePedido;
import com.plantopolis.backend.domain.model.Pedido;
import com.plantopolis.backend.domain.port.out.NotificacionPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Adaptador de notificaciones por correo electrónico.
 *
 * Implementa el puerto de salida NotificacionPort usando JavaMail + Gmail SMTP.
 * Envía dos tipos de correos:
 *   1. Confirmación de pedido → cuando el cliente hace checkout exitoso
 *   2. Cambio de estado → cuando el admin actualiza el estado del pedido
 *
 * Configuración requerida en application.yml:
 *   spring.mail.host=smtp.gmail.com
 *   spring.mail.port=587
 *   spring.mail.username=${SPRING_MAIL_USERNAME}
 *   spring.mail.password=${SPRING_MAIL_PASSWORD}  ← App Password de Gmail
 *
 * Ruta destino:
 *   Back/src/main/java/com/plantopolis/backend/
 *   infrastructure/adapter/out/notification/EmailAdapter.java
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailAdapter implements NotificacionPort {

    // JavaMailSender configurado automáticamente por Spring Boot
    // con las propiedades spring.mail.* del application.yml
    private final JavaMailSender mailSender;

    // Correo remitente leído del application.yml / .env
    // Se muestra como "De: Plantopolis <correo@gmail.com>" en el cliente de correo
    @Value("${spring.mail.username}")
    private String mailFrom;

    // Formateador de fecha para mostrar en los emails: "15 de diciembre de 2024 10:30"
    private static final DateTimeFormatter FECHA_FORMATO =
            DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy HH:mm",
                    java.util.Locale.forLanguageTag("es-CO"));

    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Envía el email de confirmación cuando se crea un pedido exitosamente.
     *
     * Contiene:
     *   - Número de pedido y fecha
     *   - Tabla con todos los productos comprados
     *   - Total del pedido
     *   - Dirección de envío
     *   - Método y estado del pago
     *
     * @param pedido dominio con toda la información del pedido recién creado
     */
    @Override
    public void enviarConfirmacionPedido(Pedido pedido) {
        try {
            log.info("Enviando email de confirmación para pedido: {} a: {}",
                    pedido.getNumeroPedido(), pedido.getEmailCliente());

            // Crear el mensaje MIME (formato que soporta HTML y múltiples partes)
            var mensaje = mailSender.createMimeMessage();

            // Helper para configurar el mensaje fácilmente
            // true = multipart (necesario para HTML)
            // "UTF-8" = soporte de tildes, ñ y caracteres especiales en español
            var helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            // Configurar remitente con nombre amigable
            helper.setFrom(mailFrom, "🌱 Plantopolis");

            // Destinatario: el email del cliente registrado
            helper.setTo(pedido.getEmailCliente());

            // Asunto del correo
            helper.setSubject("✅ Pedido " + pedido.getNumeroPedido()
                    + " confirmado — Plantopolis");

            // Cuerpo del correo en HTML (true = es HTML, no texto plano)
            helper.setText(construirHtmlConfirmacion(pedido), true);

            // Enviar el mensaje a través del servidor SMTP de Gmail
            mailSender.send(mensaje);

            log.info("✅ Email de confirmación enviado exitosamente a: {}",
                    pedido.getEmailCliente());

        } catch (Exception e) {
            // El error se loguea pero NO se relanza para no cancelar la transacción
            // El pedido queda confirmado aunque el email falle
            log.error("❌ Error enviando email de confirmación (pedido: {}): {}",
                    pedido.getNumeroPedido(), e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Envía el email de notificación cuando el admin cambia el estado del pedido.
     *
     * Estados posibles: PENDIENTE → PREPARANDO → ENVIADO → ENTREGADO / CANCELADO
     * Cada estado tiene su propio emoji y mensaje personalizado.
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

            var mensaje = mailSender.createMimeMessage();
            var helper  = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(mailFrom, "🌱 Plantopolis");
            helper.setTo(pedido.getEmailCliente());

            // Asunto dinámico según el nuevo estado del pedido
            helper.setSubject(obtenerAsuntoPorEstado(pedido.getEstadoDescripcion())
                    + " — Pedido " + pedido.getNumeroPedido());

            helper.setText(construirHtmlCambioEstado(pedido), true);

            mailSender.send(mensaje);

            log.info("✅ Email de cambio de estado '{}' enviado a: {}",
                    pedido.getEstadoDescripcion(), pedido.getEmailCliente());

        } catch (Exception e) {
            log.error("❌ Error enviando email de cambio de estado (pedido: {}): {}",
                    pedido.getNumeroPedido(), e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PLANTILLAS HTML
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Construye el HTML completo del email de confirmación de pedido.
     *
     * Incluye:
     *   - Header verde con logo de Plantopolis
     *   - Mensaje de bienvenida personalizado con el nombre del cliente
     *   - Badge con el número de pedido
     *   - Tabla de productos (nombre, cantidad, precio unitario, subtotal)
     *   - Resumen: subtotal, total y método de pago
     *   - Dirección de envío
     *   - Footer con información de contacto
     *
     * @param pedido datos del pedido para llenar la plantilla
     * @return HTML completo como String
     */
    private String construirHtmlConfirmacion(Pedido pedido) {

        // Formatear la fecha del pedido en español
        String fechaFormateada;
        if (pedido.getFechaPedido() != null) {
            ZonedDateTime colombiaTime = pedido.getFechaPedido()
                .atZone(ZoneOffset.UTC)                 // asume que está en UTC
                .withZoneSameInstant(ZoneId.of("America/Bogota")); // convierte a COL
            fechaFormateada = colombiaTime.format(FECHA_FORMATO);
        } else {
            fechaFormateada = "Fecha no disponible";
        }

        // ── INICIO DEL HTML ───────────────────────────────────────────────────
        StringBuilder html = new StringBuilder();

        // Contenedor principal con estilos inline (compatibilidad con clientes de correo)
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
            
              <!-- Contenedor centrado -->
              <table width="100%%" cellpadding="0" cellspacing="0"
                     style="background-color:#f4f7f4;padding:20px 0;">
                <tr>
                  <td align="center">
                    <table width="600" cellpadding="0" cellspacing="0"
                           style="max-width:600px;width:100%%;">
            """);

        // ── HEADER ────────────────────────────────────────────────────────────
        html.append("""
              <!-- Header verde con logo -->
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

        // ── CUERPO PRINCIPAL ──────────────────────────────────────────────────
        html.append("""
              <!-- Cuerpo blanco -->
              <tr>
                <td style="background-color:#ffffff;padding:40px;">
            """);

        // Mensaje de confirmación con ícono de éxito
        html.append(String.format("""
              <!-- Ícono de éxito y título -->
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

        // Badge con número de pedido y fecha
        html.append(String.format("""
              <!-- Badge número de pedido -->
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

        // ── TABLA DE PRODUCTOS ────────────────────────────────────────────────
        html.append("""
              <!-- Título tabla de productos -->
              <h3 style="margin:0 0 14px;color:#1a1a1a;font-size:16px;
                         font-weight:700;border-bottom:2px solid #e8f5e9;
                         padding-bottom:10px;">
                🛒 Resumen de tu pedido
              </h3>
            
              <!-- Tabla de productos -->
              <table width="100%%" cellpadding="0" cellspacing="0"
                     style="border-collapse:collapse;margin-bottom:20px;">
            
                <!-- Encabezado de la tabla -->
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

        // Filas de productos con colores alternos para legibilidad
        List<DetallePedido> detalles = pedido.getDetalles();
        if (detalles != null) {
            for (int i = 0; i < detalles.size(); i++) {
                DetallePedido detalle = detalles.get(i);
                String bgColor = (i % 2 == 0) ? "#ffffff" : "#f9fdf9";

                // Calcular subtotal del item
                BigDecimal subtotal = detalle.getSubtotal() != null
                        ? detalle.getSubtotal()
                        : detalle.getPrecioUnitario()
                                .multiply(BigDecimal.valueOf(detalle.getCantidad()));

                html.append(String.format("""
                    <tr style="background-color:%s;">
                      <!-- Nombre del producto -->
                      <td style="padding:14px 16px;border-bottom:1px solid #f0f0f0;
                                 font-size:14px;color:#333333;">
                        <span style="font-weight:600;">%s</span>
                      </td>
                      <!-- Cantidad -->
                      <td style="padding:14px 8px;border-bottom:1px solid #f0f0f0;
                                 font-size:14px;color:#555555;text-align:center;">
                        %d
                      </td>
                      <!-- Precio unitario -->
                      <td style="padding:14px 8px;border-bottom:1px solid #f0f0f0;
                                 font-size:14px;color:#555555;text-align:right;">
                        $%s
                      </td>
                      <!-- Subtotal del item -->
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

        // ── TOTAL DEL PEDIDO ──────────────────────────────────────────────────
        html.append(String.format("""
              <!-- Total del pedido -->
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

        // ── INFORMACIÓN DE PAGO Y ENVÍO ───────────────────────────────────────
        html.append("""
              <!-- Grid: pago y envío -->
              <table width="100%%" cellpadding="0" cellspacing="0"
                     style="margin-bottom:28px;">
                <tr>
            """);

        // Columna de método de pago
        String metodoPago = (pedido.getPago() != null
                && pedido.getPago().getNombreMetodo() != null)
                ? pedido.getPago().getNombreMetodo().replace("_", " ")
                : "No especificado";

        // Determinar el estado del pago según el método:
        // EFECTIVO (Contra entrega) → PENDIENTE (paga al recibir)
        // Otros métodos electrónicos → APROBADO (pago inmediato)
        String estadoPagoEmail = (pedido.getPago() != null
                && "PENDIENTE".equals(pedido.getPago().getEstadoPagoDescripcion()))
                ? "PENDIENTE" : "APROBADO";

        // Color e ícono según el estado del pago
        String colorEstadoPago = "PENDIENTE".equals(estadoPagoEmail)
                ? "#e65100"   // naranja para PENDIENTE (contra entrega)
                : "#2e7d32";  // verde para APROBADO (pago electrónico)
        String iconoEstadoPago = "PENDIENTE".equals(estadoPagoEmail)
                ? "⏳" : "✅";

        html.append(String.format("""
                  <!-- Método de pago con estado dinámico -->
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

        // Columna de dirección de envío
        html.append(String.format("""
                  <!-- Dirección de envío -->
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

        // ── MENSAJE ADICIONAL ─────────────────────────────────────────────────
        html.append("""
              <!-- Nota informativa -->
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

        // Cierre del cuerpo blanco
        html.append("</td></tr>");

        // ── FOOTER ────────────────────────────────────────────────────────────
        html.append("""
              <!-- Footer -->
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

        // Cierre de las tablas contenedoras
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

    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Construye el HTML del email de notificación de cambio de estado.
     *
     * Cada estado tiene un color y mensaje diferente:
     *   PREPARANDO → azul  — "Estamos preparando tu pedido"
     *   ENVIADO    → morado — "Tu pedido está en camino"
     *   ENTREGADO  → verde  — "¡Tu pedido fue entregado!"
     *   CANCELADO  → rojo   — "Tu pedido fue cancelado"
     *
     * @param pedido datos del pedido con el nuevo estado
     * @return HTML completo como String
     */
    private String construirHtmlCambioEstado(Pedido pedido) {

        // Determinar emoji, color y mensaje según el nuevo estado
        String estado = pedido.getEstadoDescripcion() != null
                ? pedido.getEstadoDescripcion() : "ACTUALIZADO";

        String emoji      = obtenerEmojiPorEstado(estado);
        String colorBadge = obtenerColorPorEstado(estado);
        String mensajeEstado = obtenerMensajePorEstado(estado);
        String descripcionEstado = obtenerDescripcionPorEstado(estado);

        // Formatear la fecha del pedido original
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
            
                      <!-- Header -->
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
            
                      <!-- Cuerpo -->
                      <tr>
                        <td style="background-color:#ffffff;padding:40px;">
            
                          <!-- Emoji y título del estado -->
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
            
                          <!-- Número de pedido -->
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
            
                          <!-- Badge del nuevo estado -->
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
            
                          <!-- Descripción del estado -->
                          <div style="background-color:#f0faf4;border-radius:8px;
                                      border-left:4px solid #0E4F2F;
                                      padding:20px 24px;margin-bottom:24px;">
                            <p style="margin:0;font-size:15px;color:#333333;
                                      line-height:1.7;">
                              %s
                            </p>
                          </div>
            
                          <!-- Línea de tiempo visual del pedido -->
                          <h3 style="margin:0 0 16px;color:#1a1a1a;font-size:15px;
                                     font-weight:700;">
                            📍 Seguimiento del pedido
                          </h3>
            
                          <table width="100%%" cellpadding="0" cellspacing="0"
                                 style="margin-bottom:28px;">
                            %s
                          </table>
            
                          <!-- Nota final -->
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
            
                      <!-- Footer -->
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
                emoji,                                    // %s - emoji grande
                mensajeEstado,                            // %s - título (ej: "¡Tu pedido está en camino!")
                pedido.getNombreCliente(),                // %s - nombre del cliente
                pedido.getNumeroPedido(),                 // %s - número de pedido
                fechaPedido,                             // %s - fecha del pedido original
                colorBadge,                              // %s - color del badge del estado
                emoji,                                   // %s - emoji en el badge
                estado,                                  // %s - texto del estado en el badge
                descripcionEstado,                       // %s - descripción larga del estado
                construirLineaDeTiempoHtml(estado)       // %s - línea de tiempo HTML
        );
    }

    // ═════════════════════════════════════════════════════════════════════════
    // MÉTODOS AUXILIARES
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Construye la línea de tiempo visual HTML del pedido.
     * Muestra los 4 estados principales con el estado actual resaltado.
     *
     * @param estadoActual estado actual del pedido
     * @return filas HTML de la tabla de seguimiento
     */
    private String construirLineaDeTiempoHtml(String estadoActual) {
        // Definir los pasos del flujo normal (sin CANCELADO)
        String[][] pasos = {
            {"PENDIENTE",  "⏳", "Pedido recibido",
             "Tu pedido fue recibido y está en espera de procesamiento."},
            {"PREPARANDO", "🌿", "En preparación",
             "Estamos seleccionando y empacando tus plantas con cuidado."},
            {"ENVIADO",    "🚚", "En camino",
             "Tu pedido salió de nuestras instalaciones hacia tu dirección."},
            {"ENTREGADO",  "✅", "Entregado",
             "Tu pedido fue entregado exitosamente."}
        };

        // Si el estado es CANCELADO, mostrar solo ese estado
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

        // Determinar el índice del estado actual para resaltar los completados
        int indiceActual = 0;
        for (int i = 0; i < pasos.length; i++) {
            if (pasos[i][0].equals(estadoActual)) {
                indiceActual = i;
                break;
            }
        }

        StringBuilder linea = new StringBuilder();

        for (int i = 0; i < pasos.length; i++) {
            boolean completado = i < indiceActual;   // paso ya superado
            boolean actual     = i == indiceActual;  // paso actual
            boolean pendiente  = i > indiceActual;   // paso futuro

            // Estilos según el estado del paso en la línea de tiempo
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

    /**
     * Retorna el emoji representativo según el estado del pedido.
     */
    private String obtenerEmojiPorEstado(String estado) {
        return switch (estado) {
            case "PREPARANDO" -> "🌿";
            case "ENVIADO"    -> "🚚";
            case "ENTREGADO"  -> "🎉";
            case "CANCELADO"  -> "❌";
            default           -> "📦"; // PENDIENTE u otro
        };
    }

    /**
     * Retorna el color hexadecimal del badge según el estado del pedido.
     */
    private String obtenerColorPorEstado(String estado) {
        return switch (estado) {
            case "PREPARANDO" -> "#1565C0"; // azul
            case "ENVIADO"    -> "#6A1B9A"; // morado
            case "ENTREGADO"  -> "#2E7D32"; // verde oscuro
            case "CANCELADO"  -> "#C62828"; // rojo oscuro
            default           -> "#E65100"; // naranja — PENDIENTE
        };
    }

    /**
     * Retorna el título principal del email según el estado del pedido.
     */
    private String obtenerMensajePorEstado(String estado) {
        return switch (estado) {
            case "PREPARANDO" -> "Estamos preparando tu pedido";
            case "ENVIADO"    -> "¡Tu pedido está en camino!";
            case "ENTREGADO"  -> "¡Tu pedido fue entregado!";
            case "CANCELADO"  -> "Tu pedido fue cancelado";
            default           -> "Actualización de tu pedido";
        };
    }

    /**
     * Retorna la descripción larga informativa según el estado del pedido.
     */
    private String obtenerDescripcionPorEstado(String estado) {
        return switch (estado) {
            case "PREPARANDO" ->
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

    /**
     * Retorna el asunto del correo según el estado del pedido.
     */
    private String obtenerAsuntoPorEstado(String estado) {
        return switch (estado) {
            case "PREPARANDO" -> "🌿 Estamos preparando tu pedido";
            case "ENVIADO"    -> "🚚 Tu pedido está en camino";
            case "ENTREGADO"  -> "🎉 ¡Tu pedido fue entregado!";
            case "CANCELADO"  -> "❌ Tu pedido fue cancelado";
            default           -> "📦 Actualización de tu pedido";
        };
    }

    /**
     * Formatea un BigDecimal como precio en formato colombiano.
     * Ejemplo: 25000.00 → "25.000"
     *
     * @param precio valor a formatear
     * @return string con el precio formateado
     */
    private String formatearPrecio(BigDecimal precio) {
        if (precio == null) return "0";
        // Formatear con separador de miles y sin decimales para pesos colombianos
        return String.format("%,.0f", precio);
    }
}