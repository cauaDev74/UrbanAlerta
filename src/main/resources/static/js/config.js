/**
 * config.js
 * Endereço da API. Deixe vazio quando o Spring Boot serve a interface (mesmo domínio).
 * Se o front-end ficar em outro lugar (ex.: Vercel), preencha API_PRODUCAO com a URL pública da API,
 * sem barra no final. Exemplo: "https://urbanalerta-api.onrender.com"
 */
const API_PRODUCAO = "";

const emDesenvolvimento = ["localhost", "127.0.0.1"].includes(location.hostname);
window.URBANALERTA_API = emDesenvolvimento ? "" : API_PRODUCAO;
