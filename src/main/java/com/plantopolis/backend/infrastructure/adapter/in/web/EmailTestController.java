package com.plantopolis.backend.infrastructure.adapter.in.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class EmailTestController {

    private final JavaMailSender mailSender;

    @GetMapping("/email")
    public ResponseEntity<Map<String, Object>> testEmail(
            @RequestParam String to) {

        Map<String, Object> resultado = new HashMap<>();

        try {
            log.info("Enviando email de prueba a: {}", to);

            var message = mailSender.createMimeMessage();
            var helper  = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("🌱 Plantopolis - Prueba de email");
            helper.setText("""
                <div style="font-family:Arial;max-width:500px;margin:auto;padding:20px">
                  <div style="background:#0E4F2F;padding:20px;border-radius:8px 8px 0 0;text-align:center">
                    <h1 style="color:white;margin:0">🌱 Plantopolis</h1>
                  </div>
                  <div style="border:1px solid #e0e0e0;border-top:none;padding:24px;
                              border-radius:0 0 8px 8px;text-align:center">
                    <h2 style="color:#0E4F2F">✅ Email funcionando</h2>
                    <p>El servicio de correo de Plantopolis está configurado correctamente.</p>
                  </div>
                </div>
                """, true);

            mailSender.send(message);

            log.info("✅ Email enviado a: {}", to);
            resultado.put("success", true);
            resultado.put("mensaje", "Email enviado correctamente a " + to);

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            log.error("❌ Error enviando email: {}", e.getMessage());
            resultado.put("success", false);
            resultado.put("error", e.getClass().getSimpleName());
            resultado.put("mensaje", e.getMessage());
            return ResponseEntity.status(500).body(resultado);
        }
    }
    @Autowired
    private org.springframework.core.env.Environment env;
        @GetMapping("/email/config")
    public ResponseEntity<Map<String, Object>> checkConfig() {

        Map<String, Object> config = new HashMap<>();
        String username = env.getProperty("spring.mail.username", "NO CONFIGURADO");
        String password = env.getProperty("spring.mail.password", "");

        config.put("host",    env.getProperty("spring.mail.host", "NO CONFIGURADO"));
        config.put("port",    env.getProperty("spring.mail.port", "NO CONFIGURADO"));
        config.put("usuario", username);
        config.put("password_configurado", !password.isEmpty()
                ? "✅ Sí (" + password.length() + " caracteres)"
                : "❌ NO — revisa el .env");

        return ResponseEntity.ok(config);
    }
}