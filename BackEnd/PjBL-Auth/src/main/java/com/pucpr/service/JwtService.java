package com.pucpr.service;

import com.pucpr.model.Usuario;
import io.github.cdimascio.dotenv.Dotenv;

public class JwtService {
    private final Dotenv dotenv = Dotenv.load();
    private final String SECRET_KEY = dotenv.get("JwtService");


    public String generateToken(Usuario user) {
        return "";
    }

    public String extractEmail(String token) {
        return null;
    }

    public boolean validateToken(String token) {
        return false;
    }
}