package com.plantopolis.backend.infrastructure.adapter.in.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador de diagnóstico para probar el envío de correo.
 *
 * CAMBIO: ya no usa JavaMailSender (SMTP), ahora prueba la API HTTP
 * de Resend directamente, igual que EmailAdapter.
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class EmailTestController {

    @Value("${resend.api-key}")
    private String resendApiKey;

    @Value("${resend.mail-from}")
    private String mailFrom;

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    @GetMapping("/email")
    public ResponseEntity<Map<String, Object>> testEmail(@RequestParam String to) {

        Map<String, Object> resultado = new HashMap<>();

        try {
            log.info("Enviando email de prueba (Resend) a: {}", to);

            RestClient restClient = RestClient.create();

            Map<String, Object> body = Map.of(
                    "from", "🌱 Plantopolis <" + mailFrom + ">",
                    "to", List.of(to),
                    "subject", "🌱 Plantopolis - Prueba de email",
                    "html", """
                        <div style="font-family:Arial;max-width:500px;margin:auto;padding:20px">
                          <div style="background:#0E4F2F;padding:20px;border-radius:8px 8px 0 0;text-align:center">
                            <h1 style="color:white;margin:0">🌱 Plantopolis</h1>
                          </div>
                          <div style="border:1px solid #e0e0e0;border-top:none;padding:24px;
                                      border-radius:0 0 8px 8px;text-align:center">
                            <h2 style="color:#0E4F2F">✅ Email funcionando</h2>
                            <p>El servicio de correo de Plantopolis (Resend) está configurado correctamente.</p>
                          </div>
                        </div>
                        """
            );

            var response = restClient.post()
                    .uri(RESEND_API_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + resendApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toEntity(String.class);

            log.info("✅ Email enviado a: {} — respuesta Resend: {}", to, response.getBody());
            resultado.put("success", true);
            resultado.put("mensaje", "Email enviado correctamente a " + to);
            resultado.put("resendResponse", response.getBody());

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            log.error("❌ Error enviando email: {}", e.getMessage());
            resultado.put("success", false);
            resultado.put("error", e.getClass().getSimpleName());
            resultado.put("mensaje", e.getMessage());
            return ResponseEntity.status(500).body(resultado);
        }
    }

    @GetMapping("/email/config")
    public ResponseEntity<Map<String, Object>> checkConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("proveedor", "Resend (HTTP API)");
        config.put("mailFrom", mailFrom);
        config.put("apiKeyConfigurada", resendApiKey != null && !resendApiKey.isBlank()
                ? "✅ Sí (" + resendApiKey.length() + " caracteres)"
                : "❌ NO — revisa RESEND_API_KEY en el .env");

        return ResponseEntity.ok(config);
    }
}