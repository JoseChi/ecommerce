package com.algorithm.ecommerce.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    // 1. DEFINIMOS UNA CLAVE FIJA (Debe ser larga para ser segura con HS256)
    //    En un entorno real, esto iría en application.properties, pero aquí funciona perfecto.
    private static final String SECRET_KEY = "Algorithm_Ecommerce_Clave_Super_Secreta_Para_Firmar_Tokens_12345";

    // 2. Usamos esa clave fija para crear el objeto Key
    private final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

    // El token durará 24 horas
    private final int expirationMs = 86400000;

    public String generateToken(String username, Long userId) {
        return Jwts.builder()
                .setSubject(username)
                .claim("id", userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key, SignatureAlgorithm.HS256) // Especificamos el algoritmo aquí también por seguridad
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            // Aquí caía antes porque la llave cambiaba
            System.out.println("Token inválido: " + e.getMessage());
            return false;
        }
    }
}