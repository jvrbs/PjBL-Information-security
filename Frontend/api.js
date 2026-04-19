/**
 * API integration functions with Java backend
 */

const API_BASE_URL = "http://localhost:8080/api/auth";

/**
 * Register new user
 */
async function registrarUsuario() {
    const nome = document.getElementById("nome").value;
    const email = document.getElementById("email").value;
    const senha = document.getElementById("senha").value;

    if (!nome || !email || !senha) {
        alert("Todos os campos são obrigatórios!");
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/register`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                name: nome,
                email: email,
                password: senha
            })
        });

        const data = await response.json();

        if (response.ok) {
            alert("✅ Usuário cadastrado com sucesso! Faça login agora.");
            // Clear form
            document.getElementById("nome").value = "";
            document.getElementById("email").value = "";
            document.getElementById("senha").value = "";
        } else {
            alert(`❌ Erro: ${data.message}`);
        }

    } catch (error) {
        console.error("Erro na requisição:", error);
        alert("Erro ao conectar com o servidor");
    }
}

/**
 * User login
 */
async function fazerLogin() {
    const email = document.getElementById("loginEmail").value;
    const senha = document.getElementById("loginSenha").value;

    if (!email || !senha) {
        alert("Email e senha são obrigatórios!");
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/login`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                email: email,
                password: senha
            })
        });

        const data = await response.json();

        if (response.ok) {
            // Save token to localStorage
            localStorage.setItem("authToken", data.token);
            localStorage.setItem("userEmail", email);
            
            alert("✅ Login realizado com sucesso!");
            // Redirect to dashboard
            window.location.href = "dashboard.html";
        } else {
            alert(`❌ Erro: ${data.message}`);
        }

    } catch (error) {
        console.error("Erro na requisição:", error);
        alert("Erro ao conectar com o servidor");
    }
}

/**
 * Validate token and get user data
 */
async function validarToken() {
    const token = localStorage.getItem("authToken");

    if (!token) {
        alert("Você precisa fazer login!");
        window.location.href = "index.html";
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/validate`, {
            method: "GET",
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        const data = await response.json();

        if (response.ok) {
            // Valid token, show user data
            document.getElementById("nomeUsuario").textContent = data.name;
            document.getElementById("emailUsuario").textContent = data.email;
            document.getElementById("roleUsuario").textContent = data.role;
            
            console.log("✅ Usuário autenticado:", data);
            return true;
        } else {
            // Token expired or invalid
            alert("Sua sessão expirou. Faça login novamente.");
            localStorage.removeItem("authToken");
            localStorage.removeItem("userEmail");
            window.location.href = "index.html";
            return false;
        }

    } catch (error) {
        console.error("Erro na validação:", error);
        alert("Erro ao validar sessão");
        return false;
    }
}

/**
 * User logout
 */
function fazerLogout() {
    localStorage.removeItem("authToken");
    localStorage.removeItem("userEmail");
    alert("Você saiu da conta");
    window.location.href = "index.html";
}

// Run on dashboard load to validate user session

// Get headers with authentication token for future requests
function getHeadersAutenticado() {
    const token = localStorage.getItem("authToken");
    return {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${token}`
    };
}

// Example authenticated request to other endpoints 
  async function buscarDadosSensivel() {
      const response = await fetch("http://localhost:8080/api/dados-sensivel", {
          method: "GET",
         headers: getHeadersAutenticado()
     });
  
      const data = await response.json();
      return data;
  }
 
