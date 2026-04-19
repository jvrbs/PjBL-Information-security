package com.pucpr.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pucpr.model.Usuario;
import com.pucpr.repository.UsuarioRepository;
import com.pucpr.service.JwtService;
import com.sun.net.httpserver.HttpExchange;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

public class AuthHandler {

    private final UsuarioRepository repository;
    private final JwtService jwtService;

    // JSON <-> objeto
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthHandler(UsuarioRepository repository, JwtService jwtService) {
        this.repository = repository;
        this.jwtService = jwtService;
    }

    // Login do usuário
    public void handleLogin(HttpExchange exchange) throws IOException {

        addCorsHeaders(exchange);

        // Preflight
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        try {
            String body = lerCorpoDaRequisicao(exchange);

            @SuppressWarnings("unchecked")
            Map<String, String> dados = objectMapper.readValue(body, Map.class);

            String email = dados.get("email");
            String senhaDigitada = dados.get("password");

            if (email == null || senhaDigitada == null || email.isBlank() || senhaDigitada.isBlank()) {
                enviarResposta(exchange, 400, "{\"message\": \"Email e senha são obrigatórios.\"}");
                return;
            }

            Optional<Usuario> usuarioOpt = repository.findByEmail(email);

            // valida senha com hash
            if (usuarioOpt.isEmpty() || !BCrypt.checkpw(senhaDigitada, usuarioOpt.get().getSenhaHash())) {
                enviarResposta(exchange, 401, "{\"message\": \"Credenciais inválidas.\"}");
                return;
            }

            String token = jwtService.generateToken(usuarioOpt.get());

            enviarResposta(exchange, 200, "{\"token\": \"" + token + "\"}");

        } catch (Exception e) {
            enviarResposta(exchange, 500, "{\"message\": \"Erro interno no servidor.\"}");
        }
    }

    // Cadastro de usuário
    public void handleRegister(HttpExchange exchange) throws IOException {

        addCorsHeaders(exchange);

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        try {
            String body = lerCorpoDaRequisicao(exchange);

            @SuppressWarnings("unchecked")
            Map<String, String> dados = objectMapper.readValue(body, Map.class);

            String nome  = dados.get("name");
            String email = dados.get("email");
            String senha = dados.get("password");

            if (nome == null || email == null || senha == null
                    || nome.isBlank() || email.isBlank() || senha.isBlank()) {
                enviarResposta(exchange, 400, "{\"message\": \"Nome, email e senha são obrigatórios.\"}");
                return;
            }

            // gera hash da senha
            String senhaHash = BCrypt.hashpw(senha, BCrypt.gensalt(12));

            Usuario novoUsuario = new Usuario(nome, email, senhaHash, "PACIENTE");
            repository.save(novoUsuario);

            enviarResposta(exchange, 201, "{\"message\": \"Usuário cadastrado com sucesso!\"}");

        } catch (IllegalArgumentException e) {
            // email duplicado
            enviarResposta(exchange, 409, "{\"message\": \"" + e.getMessage() + "\"}");

        } catch (Exception e) {
            enviarResposta(exchange, 500, "{\"message\": \"Erro interno no servidor.\"}");
        }
    }

    // Lê o body da requisição
    private String lerCorpoDaRequisicao(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    // Envia resposta JSON
    private void enviarResposta(HttpExchange exchange, int statusCode, String jsonBody) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    // CORS básico
    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    // Valida token JWT
    public void handleValidate(HttpExchange exchange) throws IOException {

        addCorsHeaders(exchange);

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        try {
            // header Authorization: Bearer token
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                enviarResposta(exchange, 401, "{\"message\": \"Token não fornecido ou formato inválido.\"}");
                return;
            }

            String token = authHeader.substring(7);

            if (!jwtService.validateToken(token)) {
                enviarResposta(exchange, 401, "{\"message\": \"Token inválido ou expirado.\"}");
                return;
            }

            String email = jwtService.extractEmail(token);
            Optional<Usuario> usuarioOpt = repository.findByEmail(email);

            if (usuarioOpt.isEmpty()) {
                enviarResposta(exchange, 404, "{\"message\": \"Usuário não encontrado.\"}");
                return;
            }

            Usuario usuario = usuarioOpt.get();
            String resposta = "{\"email\": \"" + usuario.getEmail() + "\", \"name\": \"" + usuario.getNome() + "\", \"role\": \"" + usuario.getRole() + "\"}";
            enviarResposta(exchange, 200, resposta);

        } catch (Exception e) {
            enviarResposta(exchange, 500, "{\"message\": \"Erro interno no servidor.\"}");
        }
    }
}