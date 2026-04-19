package com.pucpr;

import com.pucpr.handlers.AuthHandler;
import com.pucpr.repository.UsuarioRepository;
import com.pucpr.service.JwtService;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) throws IOException {

        // sobe servidor na porta 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // dependências básicas
        UsuarioRepository repository = new UsuarioRepository();
        JwtService jwtService = new JwtService();
        AuthHandler authHandler = new AuthHandler(repository, jwtService);

        // rotas de auth
        server.createContext("/api/auth/register", authHandler::handleRegister);
        server.createContext("/api/auth/login",    authHandler::handleLogin);
        server.createContext("/api/auth/validate", authHandler::handleValidate);

        // executor padrão
        server.setExecutor(null);

        System.out.println("Servidor iniciado na porta 8080...");
        server.start();
    }
}