package com.plantopolis.backend.infrastructure.adapter.out.notification;

import com.plantopolis.backend.domain.model.DetallePedido;
import com.plantopolis.backend.domain.model.Pedido;
import com.plantopolis.backend.domain.port.out.NotificacionPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailAdapter implements NotificacionPort {

    private final JavaMailSender mailSender;

    // ── Confirmación de pedido ───────────────────────────────
    @Override
    public void enviarConfirmacionPedido(Pedido pedido) {
        try {
            var msg = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(msg, true, "UTF-8");

            helper.setTo(pedido.getEmailCliente());
            helper.setSubject(
                    "🌱 Plantopolis — Pedido " + pedido.getNumeroPedido()
                    + " confirmado");
            helper.setText(buildConfirmacionHtml(pedido), true);

            mailSender.send(msg);
            log.info("Email de confirmación enviado a {}",
                    pedido.getEmailCliente());

        } catch (Exception e) {
            // No cancelar la transacción por error de email
            log.error("Error enviando email de confirmación: {}",
                    e.getMessage());
        }
    }

    // ── Cambio de estado ─────────────────────────────────────
    @Override
    public void enviarCambioDeEstado(Pedido pedido) {
        try {
            var msg = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(msg, true, "UTF-8");

            helper.setTo(pedido.getEmailCliente());
            helper.setSubject(
                    "🌱 Plantopolis — Tu pedido está "
                    + pedido.getEstadoDescripcion());
            helper.setText(buildEstadoHtml(pedido), true);

            mailSender.send(msg);
            log.info("Email de cambio de estado enviado a {}",
                    pedido.getEmailCliente());

        } catch (Exception e) {
            log.error("Error enviando email de estado: {}", e.getMessage());
        }
    }

    // ── Templates HTML ───────────────────────────────────────
    private String buildConfirmacionHtml(Pedido pedido) {
        StringBuilder sb = new StringBuilder();

        sb.append("""
            <div style="font-family:Arial,sans-serif;max-width:600px;
                        margin:auto;padding:20px">
              <div style="background:#0E4F2F;padding:20px;
                          border-radius:8px 8px 0 0;text-align:center">
                <h1 style="color:white;margin:0">🌱 Plantopolis</h1>
              </div>
              <div style="border:1px solid #e0e0e0;border-top:none;
                          padding:24px;border-radius:0 0 8px 8px">
                <h2 style="color:#0E4F2F">¡Pedido confirmado!</h2>
                <p>Hola <strong>%s</strong>, tu pedido ha sido recibido.</p>
                <div style="background:#f5f5f5;padding:12px;
                            border-radius:6px;margin:16px 0">
                  <p style="margin:0">
                    Número de pedido:
                    <strong style="color:#0E4F2F">%s</strong>
                  </p>
                </div>
                <h3 style="color:#333">Resumen de tu pedido</h3>
                <table style="width:100%%;border-collapse:collapse;
                              font-size:14px">
                  <thead>
                    <tr style="background:#0E4F2F;color:white">
                      <th style="padding:10px;text-align:left">Producto</th>
                      <th style="padding:10px;text-align:center">Cant.</th>
                      <th style="padding:10px;text-align:right">Precio</th>
                      <th style="padding:10px;text-align:right">Subtotal</th>
                    </tr>
                  </thead>
                  <tbody>
            """.formatted(
                    pedido.getNombreCliente(),
                    pedido.getNumeroPedido()));

        // Filas de productos
        boolean alterno = false;
        for (DetallePedido d : pedido.getDetalles()) {
            String bg = alterno ? "#f9f9f9" : "white";
            sb.append("""
                <tr style="background:%s">
                  <td style="padding:10px;border-bottom:1px solid #eee">
                    %s
                  </td>
                  <td style="padding:10px;text-align:center;
                             border-bottom:1px solid #eee">
                    %d
                  </td>
                  <td style="padding:10px;text-align:right;
                             border-bottom:1px solid #eee">
                    $%.2f
                  </td>
                  <td style="padding:10px;text-align:right;
                             border-bottom:1px solid #eee">
                    $%.2f
                  </td>
                </tr>
                """.formatted(
                    bg,
                    d.getNombreProducto(),
                    d.getCantidad(),
                    d.getPrecioUnitario(),
                    d.getSubtotal()));
            alterno = !alterno;
        }

        sb.append("""
                  </tbody>
                </table>
                <div style="text-align:right;margin-top:12px;
                            font-size:16px">
                  <strong>Total: $%.2f</strong>
                </div>
                <hr style="margin:20px 0"/>
                <p style="color:#555">
                  📦 <strong>Dirección de envío:</strong> %s
                </p>
                <p style="color:#888;font-size:12px;margin-top:20px">
                  Gracias por comprar en Plantopolis.
                  Si tienes preguntas, responde este correo.
                </p>
              </div>
            </div>
            """.formatted(pedido.getTotal(), pedido.getDireccionEnvio()));

        return sb.toString();
    }

    private String buildEstadoHtml(Pedido pedido) {
        String emoji = switch (pedido.getEstadoDescripcion()) {
            case "PREPARANDO" -> "🌿";
            case "ENVIADO"    -> "🚚";
            case "ENTREGADO"  -> "✅";
            case "CANCELADO"  -> "❌";
            default           -> "📦";
        };

        return """
            <div style="font-family:Arial,sans-serif;max-width:600px;
                        margin:auto;padding:20px">
              <div style="background:#0E4F2F;padding:20px;
                          border-radius:8px 8px 0 0;text-align:center">
                <h1 style="color:white;margin:0">🌱 Plantopolis</h1>
              </div>
              <div style="border:1px solid #e0e0e0;border-top:none;
                          padding:24px;border-radius:0 0 8px 8px">
                <h2 style="color:#0E4F2F">
                  %s Actualización de tu pedido
                </h2>
                <p>Hola <strong>%s</strong>,</p>
                <p>Tu pedido <strong>%s</strong> ahora está en estado:</p>
                <div style="background:#e8f5e9;padding:16px;
                            border-radius:6px;text-align:center;
                            font-size:18px;font-weight:bold;
                            color:#0E4F2F;margin:16px 0">
                  %s %s
                </div>
                <p style="color:#888;font-size:12px;margin-top:20px">
                  Plantopolis — Plantas para tu hogar
                </p>
              </div>
            </div>
            """.formatted(
                emoji,
                pedido.getNombreCliente(),
                pedido.getNumeroPedido(),
                emoji,
                pedido.getEstadoDescripcion());
    }
}