package com.plantopolis.backend.infrastructure.security;

import com.plantopolis.backend.infrastructure.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtConfig jwtConfig;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(
            jwtConfig.getSecret().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /** Genera el token JWT con email + rol como claim */
    public String generarToken(UserDetails userDetails, String rol) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("rol", rol);
        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtConfig.getExpiration()))
                .signWith(getSigningKey())
                .compact();
    }

    /** Extrae el email (subject) del token */
    public String extraerEmail(String token) {
        return parsearClaims(token).getSubject();
    }

    /** Constante que identifica el propósito de tokens temporales de recuperación de contraseña */
    public static final String PROPOSITO_RECUPERACION = "RECUPERACION_PASSWORD";
    /** Duración de 15 minutos en milisegundos para tokens temporales de reseteo */
    public static final long DURACION_RECUPERACION_MS = 15 * 60 * 1000L;

    /** Extrae el rol del token */
    public String extraerRol(String token) {
        return (String) parsearClaims(token).get("rol");
    }

    /**
     * Genera un token JWT temporal con vigencia corta (15 minutos) específicamente
     * para el flujo de autoservicio de recuperación de contraseña (HU2).
     *
     * @param email Correo electrónico del usuario que solicita la recuperación
     * @return Cadena JWT firmada con el claim de propósito correspondiente
     */
    public String generarTokenRecuperacion(String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("proposito", PROPOSITO_RECUPERACION);
        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + DURACION_RECUPERACION_MS))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Valida si un token JWT corresponde a una solicitud legítima y vigente de recuperación de contraseña.
     *
     * @param token Token JWT recibido
     * @return true si la firma es válida, no ha expirado y su propósito es RECUPERACION_PASSWORD
     */
    public boolean validarTokenRecuperacion(String token) {
        try {
            Claims claims = parsearClaims(token);
            String proposito = (String) claims.get("proposito");
            return PROPOSITO_RECUPERACION.equals(proposito) && !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token de recuperación de contraseña inválido o expirado: {}", e.getMessage());
            return false;
        }
    }

    /** Valida que el token sea válido y corresponda al usuario */
    public boolean esValido(String token, UserDetails userDetails) {
        try {
            String email = extraerEmail(token);
            return email.equals(userDetails.getUsername()) && !estaExpirado(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token JWT inválido: {}", e.getMessage());
            return false;
        }
    }

    public boolean estaVigente(String token) {
        try {
            return !estaExpirado(token);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private boolean estaExpirado(String token) {
        return parsearClaims(token).getExpiration().before(new Date());
    }

    private Claims parsearClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
