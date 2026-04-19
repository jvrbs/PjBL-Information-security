package com.pucpr.service;

import com.pucpr.model.Usuario;
import io.github.cdimascio.dotenv.Dotenv;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtService {

    // carrega chave do .env
    private final Dotenv dotenv = Dotenv.load();
    private final String SECRET_KEY = dotenv.get("JWT_SECRET");

    // 1h de expiração
    private static final long EXPIRATION_MS = 1000L * 60 * 60;

    private SecretKey getSigningKey() {
        byte[] keyBytes = SECRET_KEY.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // gera token com dados básicos do usuário
    public String generateToken(Usuario user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + EXPIRATION_MS);

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole())
                .claim("name", user.getNome())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    // extrai email do token
    public String extractEmail(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getSubject();

        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    // valida assinatura e expiração
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);

            return true;

        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}