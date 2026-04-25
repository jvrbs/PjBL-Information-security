# 🏥 HealthAuth

Plataforma de autenticação segura com JWT, construída com **Java puro** (sem frameworks) no backend e **HTML/JS vanilla** no frontend.

---

## 📋 Índice

- [Visão Geral](#visão-geral)
- [Tecnologias](#tecnologias)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Configuração](#configuração)
- [Endpoints](#endpoints)
- [Referência das Classes](#referência-das-classes)
- [Frontend](#frontend)
- [Fluxo Completo](#fluxo-completo)

---

## Visão Geral

```
Cliente (Browser) ←→ Java HttpServer :8080 ←→ usuarios.json
```

Usuários são persistidos em um arquivo JSON local. O sistema implementa registro, login e validação de sessão via JWT com senhas armazenadas em hash BCrypt.

---

## Tecnologias

### Backend
| Dependência | Versão | Uso |
|---|---|---|
| Java (JDK embutido) | 17 | `HttpServer` — servidor HTTP sem framework |
| JJWT | 0.12.5 | Geração e validação de tokens JWT |
| jBCrypt | 0.4 | Hash de senhas |
| Jackson | 2.17.0 | Serialização/desserialização JSON |
| dotenv-java | 3.0.0 | Leitura de variáveis de ambiente via `.env` |

### Frontend
- HTML5 + CSS3 + JavaScript Vanilla
- Sem frameworks ou dependências externas

---

## Estrutura do Projeto

```
PjBL-Auth/
├── .env                          # Variáveis de ambiente (não commitado)
├── pom.xml
├── usuarios.json                 # Banco de dados em arquivo
└── src/main/java/com/pucpr/
    ├── Main.java                 # Ponto de entrada
    ├── model/
    │   └── Usuario.java          # Entidade de usuário
    ├── repository/
    │   └── UsuarioRepository.java
    ├── service/
    │   └── JwtService.java
    └── handlers/
        └── AuthHandler.java

Frontend/
├── index.html                    # Login e cadastro
├── dashboard.html                # Área autenticada
└── api.js                        # Funções de integração com a API
```

---

## Configuração

Crie um arquivo `.env` na raiz do projeto backend:

```env
JWT_SECRET=sua_chave_secreta_com_no_minimo_32_caracteres
```

> A chave deve ter ao menos 32 caracteres para garantir segurança com HMAC-SHA256.

Com o Maven, compile e execute:

```bash
mvn compile exec:java -Dexec.mainClass="com.pucpr.Main"
```

O servidor sobe na porta `8080`.

---

## Endpoints

### `POST /api/auth/register`

Cadastra um novo usuário.

**Body:**
```json
{
  "name": "Maria da Silva",
  "email": "maria@email.com",
  "password": "Senha123"
}
```

**Respostas:**

| Status | Descrição |
|---|---|
| `201` | Usuário cadastrado com sucesso |
| `400` | Campos obrigatórios ausentes |
| `409` | E-mail já cadastrado |
| `500` | Erro interno |

---

### `POST /api/auth/login`

Autentica o usuário e retorna um JWT.

**Body:**
```json
{
  "email": "maria@email.com",
  "password": "Senha123"
}
```

**Resposta `200`:**
```json
{
  "token": "eyJhbGci..."
}
```

| Status | Descrição |
|---|---|
| `200` | Login bem-sucedido, retorna token |
| `400` | Campos ausentes |
| `401` | Credenciais inválidas |

---

### `GET /api/auth/validate`

Valida um token JWT e retorna os dados do usuário autenticado.

**Header:** `Authorization: Bearer <token>`

**Resposta `200`:**
```json
{
  "email": "maria@email.com",
  "name": "Maria da Silva",
  "role": "PACIENTE"
}
```

| Status | Descrição |
|---|---|
| `200` | Token válido, retorna dados do usuário |
| `401` | Token ausente, malformado ou expirado |
| `404` | Usuário do token não encontrado |

---

## Referência das Classes

### `Main.java`

Ponto de entrada da aplicação. Inicializa o servidor e conecta as dependências manualmente (sem injeção automática de dependência).

#### `main(String[] args)`
- Cria o `HttpServer` na porta `8080`
- Instancia `UsuarioRepository`, `JwtService` e `AuthHandler`
- Registra as rotas com `server.createContext()`
- Chama `server.start()`

| Rota | Handler |
|---|---|
| `/api/auth/register` | `authHandler::handleRegister` |
| `/api/auth/login` | `authHandler::handleLogin` |
| `/api/auth/validate` | `authHandler::handleValidate` |

---

### `model/Usuario.java`

Entidade que representa um usuário. Anotada com `@JsonIgnoreProperties(ignoreUnknown = true)` para ignorar campos desconhecidos ao ler o JSON.

#### Campos

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | `String` | UUID gerado automaticamente no construtor |
| `nome` | `String` | Nome completo |
| `email` | `String` | Identificador único do usuário |
| `senhaHash` | `String` | Hash BCrypt da senha (`@JsonProperty("senhaHash")`) |
| `role` | `String` | Perfil do usuário: `PACIENTE`, `MEDICO`, `ADMIN` |

#### Métodos

| Método | Descrição |
|---|---|
| `Usuario()` | Construtor vazio — obrigatório para o Jackson desserializar |
| `Usuario(nome, email, senhaHash, role)` | Construtor principal — gera `id` via `UUID.randomUUID()` |
| Getters e Setters | Para todos os cinco campos |
| `toString()` | Retorna apenas `email` e `role`, omitindo o hash por segurança |

**Usada em:** todas as rotas.

---

### `repository/UsuarioRepository.java`

Camada de persistência. Lê e grava usuários no arquivo `usuarios.json` via Jackson.

#### Métodos

#### `findAll() → List<Usuario>`
- Verifica se `usuarios.json` existe — retorna lista vazia se não existir
- Desserializa o arquivo completo em `List<Usuario>` usando `TypeReference`
- Em caso de `IOException`, retorna lista vazia silenciosamente

**Usada em:** `findByEmail()` e `save()`.

---

#### `findByEmail(String email) → Optional<Usuario>`
- Chama `findAll()` e filtra por email com `equalsIgnoreCase` (case-insensitive)
- Retorna `Optional.empty()` se não encontrar

**Usada nas rotas:** `POST /login` e `GET /validate`.

---

#### `save(Usuario usuario) throws IOException`
- Chama `findAll()` para carregar a lista atual
- Verifica duplicidade de email — lança `IllegalArgumentException` se já existir
- Adiciona o usuário à lista e reescreve o arquivo inteiro com `writerWithDefaultPrettyPrinter()`

**Usada na rota:** `POST /register`.

---

### `service/JwtService.java`

Toda a lógica de JWT. Usa a API fluente do JJWT 0.12.5.

#### Configuração interna

| Parâmetro | Valor |
|---|---|
| Chave secreta | Lida do `.env` via `Dotenv.load()` |
| Algoritmo | HMAC-SHA256 (implícito pelo `Keys.hmacShaKeyFor`) |
| Expiração | 1 hora (`1000ms × 60 × 60`) |

#### Métodos

#### `getSigningKey() → SecretKey` *(privado)*
- Converte `SECRET_KEY` em bytes UTF-8
- Retorna `SecretKey` via `Keys.hmacShaKeyFor(bytes)`

**Usada em:** `generateToken()`, `validateToken()`, `extractEmail()`.

---

#### `generateToken(Usuario user) → String`

Gera um JWT assinado com os seguintes claims:

| Claim | Valor |
|---|---|
| `sub` | `user.getEmail()` |
| `role` | `user.getRole()` |
| `name` | `user.getNome()` |
| `iat` | Timestamp atual |
| `exp` | `iat` + 1 hora |

**Usada na rota:** `POST /login`.

---

#### `extractEmail(String token) → String`
- Parseia o token com `Jwts.parser().verifyWith(...).build().parseSignedClaims(token)`
- Retorna `claims.getSubject()` (o campo `sub`, que é o email)
- Retorna `null` em caso de `JwtException` ou `IllegalArgumentException`

**Usada na rota:** `GET /validate`.

---

#### `validateToken(String token) → boolean`
- Tenta parsear e verificar assinatura + expiração do token
- Retorna `true` se válido, `false` se qualquer exceção for lançada
- A expiração é verificada automaticamente pelo JJWT

**Usada na rota:** `GET /validate`.

---

### `handlers/AuthHandler.java`

Camada de apresentação (equivalente a um Controller). Recebe `HttpExchange`, delega para `JwtService` e `UsuarioRepository`, e escreve a resposta.

Recebe as dependências pelo construtor:
```java
public AuthHandler(UsuarioRepository repository, JwtService jwtService)
```

#### Métodos Públicos

#### `handleRegister(HttpExchange exchange)` → `POST /api/auth/register`

```
1. addCorsHeaders()
2. Rejeita métodos diferentes de POST → 405
3. lerCorpoDaRequisicao() → lê body como String
4. Jackson desserializa em Map<String, String>
5. Extrai: name, email, password
6. Valida se algum campo está nulo/em branco → 400
7. BCrypt.hashpw(senha, BCrypt.gensalt(12)) → gera hash
8. new Usuario(nome, email, senhaHash, "PACIENTE")
9. repository.save(usuario)
   └─ IllegalArgumentException (email duplicado) → 409
10. enviarResposta(201, { "message": "Usuário cadastrado com sucesso!" })
```

> O `role` é sempre `"PACIENTE"` — não é possível definir outro perfil pelo cadastro público.

---

#### `handleLogin(HttpExchange exchange)` → `POST /api/auth/login`

```
1. addCorsHeaders()
2. Rejeita métodos diferentes de POST → 405
3. lerCorpoDaRequisicao() → extrai email e password
4. Valida campos → 400 se em branco
5. repository.findByEmail(email) → Optional<Usuario>
6. BCrypt.checkpw(senhaDigitada, hash)
   └─ Optional.empty() ou senha errada → 401
   └─ Mesma mensagem para ambos os casos (não revela qual campo errou)
7. jwtService.generateToken(usuario) → String token
8. enviarResposta(200, { "token": "..." })
```

---

#### `handleValidate(HttpExchange exchange)` → `GET /api/auth/validate`

```
1. addCorsHeaders()
2. Rejeita métodos diferentes de GET → 405
3. Lê header Authorization
4. Valida se existe e começa com "Bearer " → 401
5. token = authHeader.substring(7)
6. jwtService.validateToken(token) → false → 401
7. jwtService.extractEmail(token) → email
8. repository.findByEmail(email) → Optional.empty() → 404
9. enviarResposta(200, { email, name, role })
```

---

#### Métodos Privados (utilitários)

| Método | Descrição |
|---|---|
| `lerCorpoDaRequisicao(HttpExchange)` | Lê `getRequestBody()` inteiro como String UTF-8 via `readAllBytes()` |
| `enviarResposta(HttpExchange, int, String)` | Define `Content-Type: application/json`, converte body para bytes e escreve na resposta |
| `addCorsHeaders(HttpExchange)` | Adiciona `Access-Control-Allow-Origin: *` e headers relacionados. Cada handler também trata `OPTIONS` retornando `204` para suporte ao preflight do browser |

---

## Mapa Rota → Métodos

| Rota | Handler | Repository | Service |
|---|---|---|---|
| `POST /register` | `handleRegister` | `save()` | — |
| `POST /login` | `handleLogin` | `findByEmail()` | `generateToken()` |
| `GET /validate` | `handleValidate` | `findByEmail()` | `validateToken()`, `extractEmail()` |

---

## Frontend

Duas páginas estáticas sem framework:

| Arquivo | Função |
|---|---|
| `index.html` | Formulários de login e cadastro com validação client-side |
| `dashboard.html` | Área autenticada — exibe dados e anatomia do JWT |
| `api.js` | Funções de integração com a API (registro, login, validação, logout) |

### Armazenamento Seguro do Token

O token **nunca vai para `localStorage`** (vulnerável a XSS). O fluxo é:

```
Login → AuthStore._token (memória RAM)
      → sessionStorage como ponte temporária para mudança de página
      → Dashboard lê e apaga o sessionStorage imediatamente
      → Token fica apenas em memória
      → Fechar a aba = token descartado ✅
```

### AuthStore — Padrão Module/IIFE

```js
const AuthStore = (() => {
  let _token = null; // privado por closure
  return {
    set(t)   { _token = t; },
    get()    { return _token; },
    clear()  { _token = null; },
    isAuth() { return !!_token; }
  };
})();
```

### Anatomia do JWT no Dashboard

O dashboard decodifica o JWT com `atob()` **sem verificar a assinatura** — apenas para exibição didática. Demonstra que o payload é público (qualquer um pode ler), mas não forjável sem a chave secreta.

---

## Fluxo Completo

```
1.  Usuário preenche o form de login
2.  Frontend valida campos localmente
3.  POST /api/auth/login → { email, password }
4.  Backend: findByEmail → BCrypt.checkpw → generateToken
5.  Retorna { token: "header.payload.signature" }
6.  Frontend: AuthStore.set(token) + sessionStorage como ponte
7.  Redirect para dashboard.html
8.  Dashboard lê sessionStorage, move para memória, apaga storage
9.  Renderiza dados decodificados do payload JWT
10. Requisições futuras usam: Authorization: Bearer <token>
```
