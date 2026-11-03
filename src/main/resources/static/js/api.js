/**
 * api.js
 * Única camada do front-end que conversa com o servidor Java.
 * Envia o token JWT (guardado na sessionStorage) em todas as chamadas.
 */
// Vazio = mesma origem (o Spring Boot serve a interface e a API juntos). Veja js/config.js.
const API_BASE = window.URBANALERTA_API || "";
const CHAVE_TOKEN = "urbanalerta.token";

/** Definido por auth.js: o que fazer quando o servidor diz que a sessão acabou. */
let aoSessaoExpirada = () => {};

function lerToken() {
  return sessionStorage.getItem(CHAVE_TOKEN);
}

async function chamar(caminho, opcoes = {}) {
  const cabecalhos = { "Content-Type": "application/json" };
  const token = lerToken();
  if (token) cabecalhos.Authorization = "Bearer " + token;

  let resposta;
  try {
    resposta = await fetch(API_BASE + caminho, { ...opcoes, headers: cabecalhos });
  } catch (e) {
    throw new Error("Não foi possível falar com o servidor. Ele está rodando?");
  }
  if (!resposta.ok) {
    let mensagem = "Erro inesperado (" + resposta.status + ").";
    try {
      const corpo = await resposta.json();
      if (corpo && corpo.erro) mensagem = corpo.erro;
    } catch (_) { /* resposta sem corpo JSON */ }
    if (resposta.status === 401 && token) aoSessaoExpirada(mensagem);
    throw new Error(mensagem);
  }
  return resposta.json();
}

const api = {
  entrar: (email, senha) => chamar("/auth/login", { method: "POST", body: JSON.stringify({ email, senha }) }),
  cadastrar: (nome, email, senha) => chamar("/auth/cadastro", { method: "POST", body: JSON.stringify({ nome, email, senha }) }),
  eu: () => chamar("/auth/eu"),
  sistema: () => chamar("/sistema"),
  tipos: () => chamar("/tipos-ocorrencia"),
  listar: () => chamar("/ocorrencias"),
  buscar: (id) => chamar("/ocorrencias/" + id),
  criar: (dados) => chamar("/ocorrencias", { method: "POST", body: JSON.stringify(dados) }),
  atualizarStatus: (id, status) =>
    chamar("/ocorrencias/" + id + "/status", { method: "PATCH", body: JSON.stringify({ status }) }),
};
