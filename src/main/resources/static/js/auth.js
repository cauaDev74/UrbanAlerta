/**
 * auth.js
 * Tela de acesso (entrar / criar conta) e controle da sessão.
 * O token JWT fica na sessionStorage: some quando a aba é fechada.
 */
const CHAVE_AVISO = "urbanalerta.aviso";
const CONTAS_DEMO = {
  cliente: ["cliente@urbanalerta.local", "cliente123"],
  admin: ["admin@urbanalerta.local", "admin123"],
};

function sair() {
  sessionStorage.removeItem(CHAVE_TOKEN);
  location.reload();
}

function mostrarAcesso() {
  $("app").hidden = true;
  $("acesso").hidden = false;
  const aviso = sessionStorage.getItem(CHAVE_AVISO);
  if (aviso) {
    $("aviso-acesso").textContent = aviso;
    $("aviso-acesso").hidden = false;
    sessionStorage.removeItem(CHAVE_AVISO);
  }
}

function mostrarErroAcesso(id, mensagem) {
  const el = $(id);
  el.textContent = mensagem || "";
  el.hidden = !mensagem;
}

function trocarAba(aba) {
  const entrar = aba === "entrar";
  $("form-entrar").hidden = !entrar;
  $("form-cadastro").hidden = entrar;
  $("aba-entrar").setAttribute("aria-selected", String(entrar));
  $("aba-cadastro").setAttribute("aria-selected", String(!entrar));
  mostrarErroAcesso("erro-entrar", "");
  mostrarErroAcesso("erro-cadastro", "");
}

async function concluirAcesso(resposta) {
  sessionStorage.setItem(CHAVE_TOKEN, resposta.token);
  await iniciarApp(resposta.usuario);
}

async function enviarAcesso(botao, rotuloOcupado, rotuloNormal, idErro, chamada) {
  mostrarErroAcesso(idErro, "");
  botao.disabled = true;
  botao.textContent = rotuloOcupado;
  try {
    await concluirAcesso(await chamada());
  } catch (erro) {
    mostrarErroAcesso(idErro, erro.message);
  } finally {
    botao.disabled = false;
    botao.textContent = rotuloNormal;
  }
}

function ligarAcesso() {
  document.querySelectorAll("[data-aba]").forEach((b) => b.addEventListener("click", () => trocarAba(b.dataset.aba)));

  $("form-entrar").addEventListener("submit", (e) => {
    e.preventDefault();
    const email = $("entrar-email").value.trim();
    const senha = $("entrar-senha").value;
    if (!email || !senha) { mostrarErroAcesso("erro-entrar", "Informe e-mail e senha."); return; }
    enviarAcesso($("btn-entrar"), "Entrando…", "Entrar", "erro-entrar", () => api.entrar(email, senha));
  });

  $("form-cadastro").addEventListener("submit", (e) => {
    e.preventDefault();
    const nome = $("cad-nome").value.trim();
    const email = $("cad-email").value.trim();
    const senha = $("cad-senha").value;
    if (!nome || !email || !senha) { mostrarErroAcesso("erro-cadastro", "Preencha nome, e-mail e senha."); return; }
    if (senha.length < 6) { mostrarErroAcesso("erro-cadastro", "A senha deve ter pelo menos 6 caracteres."); return; }
    enviarAcesso($("btn-cadastro"), "Criando conta…", "Criar conta", "erro-cadastro", () => api.cadastrar(nome, email, senha));
  });

  document.querySelectorAll("[data-demo]").forEach((b) => b.addEventListener("click", () => {
    const [email, senha] = CONTAS_DEMO[b.dataset.demo];
    trocarAba("entrar");
    $("entrar-email").value = email;
    $("entrar-senha").value = senha;
    $("form-entrar").requestSubmit();
  }));
}

document.addEventListener("DOMContentLoaded", async () => {
  aoSessaoExpirada = (mensagem) => {
    sessionStorage.removeItem(CHAVE_TOKEN);
    sessionStorage.setItem(CHAVE_AVISO, mensagem);
    location.reload();
  };
  ligarAcesso();
  api.sistema().then((info) => { $("demo").hidden = !info.demonstracao; }).catch(() => {});

  if (lerToken()) {
    try {
      await iniciarApp(await api.eu());
      return;
    } catch (erro) {
      sessionStorage.removeItem(CHAVE_TOKEN);
    }
  }
  mostrarAcesso();
});
