/**
 * app.js
 * Aplicativo depois do login. Mesmo código para os dois perfis; o que aparece muda conforme o perfil:
 *  - CLIENTE: mapa, registrar ocorrência, minhas ocorrências e detalhe (somente leitura);
 *  - ADMIN: mapa, gestão (indicadores + fila) e detalhe com os botões que mudam o status.
 * Todos os dados vêm da API Java (api.js). Aqui só existe lógica de apresentação.
 */
const ROTULO_STATUS = { PENDENTE: "Pendente", EM_ANALISE: "Em análise", RESOLVIDO: "Resolvido" };
const ROTULO_ACAO = { EM_ANALISE: "Iniciar análise", RESOLVIDO: "Marcar como resolvida" };
const ORDEM_STATUS = ["PENDENTE", "EM_ANALISE", "RESOLVIDO"];
const COR_STATUS = { PENDENTE: "var(--ambar)", EM_ANALISE: "var(--azul)", RESOLVIDO: "var(--ok)" };

const estado = {
  usuario: null,
  ocorrencias: [],
  filtro: null,          // status escolhido nos chips acima do mapa
  selecionadaId: null,   // ocorrência destacada no mapa
  detalheId: null,
  tela: "mapa",
  origemDetalhe: "mapa",
  mapaIniciado: false,
  eventosLigados: false,
};

const $ = (id) => document.getElementById(id);
const ehAdmin = () => !!estado.usuario && estado.usuario.perfil === "ADMIN";

function esc(texto) {
  return String(texto ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}

function formatarDataHora(iso) {
  return new Date(iso).toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
}

function textoDias(o) {
  if (o.status === "RESOLVIDO") return o.diasEmAberto === 1 ? "Resolvida em 1 dia" : "Resolvida em " + o.diasEmAberto + " dias";
  if (o.diasEmAberto === 0) return "Registrada hoje";
  return o.diasEmAberto === 1 ? "1 dia em aberto" : o.diasEmAberto + " dias em aberto";
}

function pilula(o) {
  return '<span class="pilula cor-' + o.status + '">' + esc(o.statusDescricao) + "</span>";
}

let temporizadorAviso;
function avisar(mensagem, erro) {
  const el = $("aviso");
  el.textContent = mensagem;
  el.className = "aviso" + (erro ? " erro-aviso" : "");
  el.hidden = false;
  clearTimeout(temporizadorAviso);
  temporizadorAviso = setTimeout(() => { el.hidden = true; }, 3800);
}

/* ---------- Dados filtrados ---------- */
function visiveis() {
  return estado.filtro ? estado.ocorrencias.filter((o) => o.status === estado.filtro) : estado.ocorrencias;
}

/* ---------- Navegação ---------- */
/** No computador o painel fica ao lado do mapa; na aba "Mapa" ele mostra a lista padrão do perfil. */
function telaDoPainel(nome) {
  if (nome !== "mapa") return nome;
  return ehAdmin() ? "fila" : "minhas";
}

function abrirTela(nome) {
  estado.tela = nome;
  $("app").dataset.tela = nome;
  const painel = telaDoPainel(nome);
  document.querySelectorAll(".painel .tela").forEach((t) => t.classList.toggle("ativa", t.id === "tela-" + painel));
  const abaAtiva = nome === "detalhe" ? estado.origemDetalhe : nome;
  document.querySelectorAll(".nav button").forEach((b) => b.classList.toggle("ativo", b.dataset.tela === abaAtiva));
  Mapa.redimensionar();
}

function abrirDetalhe(id) {
  estado.origemDetalhe = estado.tela === "detalhe" ? estado.origemDetalhe : estado.tela;
  estado.detalheId = id;
  renderDetalhe();
  abrirTela("detalhe");
}

function voltar() {
  abrirTela(estado.tela === "detalhe" ? estado.origemDetalhe : "mapa");
}

function selecionar(id, focar) {
  estado.selecionadaId = id;
  renderCartao();
  Mapa.desenhar(visiveis(), id, false);
  const o = estado.ocorrencias.find((x) => x.id === id);
  if (focar && o) Mapa.focar(o.latitude, o.longitude);
}

function aplicarFiltro(valor) {
  estado.filtro = (valor === "" || valor === estado.filtro) ? null : valor;
  renderTudo(false);
}

/* ---------- Renderização ---------- */
function renderResumo() {
  const rotulo = { PENDENTE: "pendentes", EM_ANALISE: "em análise", RESOLVIDO: "resolvidas" };
  const chips = ORDEM_STATUS.map((s) => {
    const n = estado.ocorrencias.filter((o) => o.status === s).length;
    const ativo = estado.filtro === s;
    return '<button type="button" class="chip' + (ativo ? " ativo" : "") + '" data-filtro="' + s + '" aria-pressed="' + ativo + '">' +
      '<i class="ponto" style="background:' + COR_STATUS[s] + '"></i>' + n + " " + rotulo[s] + "</button>";
  }).join("");
  $("resumo").innerHTML = chips + (estado.filtro ? '<button type="button" class="chip limpar" data-filtro="">Limpar filtro</button>' : "");
}

/** Cartão de uma ocorrência. O administrador vê o autor e o botão da próxima etapa. */
function htmlCard(o, opcoes) {
  const comMapa = !!(opcoes && opcoes.comMapa);
  const proximo = o.proximosStatus && o.proximosStatus[0];
  const acao = (ehAdmin() && proximo)
    ? '<button type="button" class="btn-acao" data-acao="status" data-id="' + esc(o.id) + '" data-status="' + esc(proximo.valor) + '">' + ROTULO_ACAO[proximo.valor] + "</button>"
    : "";
  const autor = (ehAdmin() && o.autorNome) ? "<span>Por " + esc(o.autorNome) + "</span>" : "";
  return '<article class="card' + (o.id === estado.selecionadaId ? " sel" : "") + '">' +
    '<div class="card-topo"><strong>' + esc(o.tipoDescricao) + "</strong>" + pilula(o) + "</div>" +
    '<p class="endereco">' + esc(o.endereco) + "</p>" +
    '<div class="card-meta"><span>' + esc(textoDias(o)) + "</span>" + autor + "<span>" + esc(formatarDataHora(o.dataHoraRegistro)) + "</span></div>" +
    '<div class="card-acoes">' +
      '<button type="button" class="contorno" data-acao="detalhe" data-id="' + esc(o.id) + '">Detalhes</button>' +
      (comMapa ? '<button type="button" class="contorno" data-acao="mapa" data-id="' + esc(o.id) + '">Ver no mapa</button>' : "") +
      acao +
    "</div></article>";
}

function renderCartao() {
  const o = visiveis().find((x) => x.id === estado.selecionadaId);
  if (!o) {
    $("cartao").innerHTML = '<div class="vazio">' + (estado.ocorrencias.length
      ? "Toque em um ponto do mapa para ver a ocorrência."
      : "Nenhuma ocorrência registrada ainda.") + "</div>";
    return;
  }
  $("cartao").innerHTML = htmlCard(o, { comMapa: false });
}

function renderMinhas() {
  const minhas = visiveis().filter((o) => o.minha);
  if (minhas.length === 0) {
    $("minhas").innerHTML = '<div class="vazio">' +
      (estado.filtro ? "Nenhuma ocorrência sua com este status." : "Você ainda não registrou nenhuma ocorrência.") +
      '<br><button type="button" class="primario pequeno" data-ir="registrar">Registrar ocorrência</button></div>';
    return;
  }
  $("minhas").innerHTML = '<div class="lista">' + minhas.map((o) => htmlCard(o, { comMapa: true })).join("") + "</div>";
}

function htmlKpis() {
  const todas = estado.ocorrencias;
  const contar = (s) => todas.filter((o) => o.status === s).length;
  const resolvidas = todas.filter((o) => o.status === "RESOLVIDO");
  const media = resolvidas.length ? resolvidas.reduce((soma, o) => soma + o.diasEmAberto, 0) / resolvidas.length : null;
  const mediaTexto = media === null ? "—" : media.toLocaleString("pt-BR", { maximumFractionDigits: 1 });
  const kpi = (valor, rotulo, cor) => '<div class="kpi"><strong' + (cor ? ' style="color:' + cor + '"' : "") + ">" + valor + "</strong><span>" + rotulo + "</span></div>";
  return '<div class="kpis">' +
    kpi(todas.length, "Total") + kpi(contar("PENDENTE"), "Pendentes", "var(--ambar-texto)") +
    kpi(contar("EM_ANALISE"), "Em análise", "var(--azul)") + kpi(contar("RESOLVIDO"), "Resolvidas", "var(--ok)") +
    kpi(mediaTexto, "Dias até resolver (média)") + "</div>";
}

function renderFila() {
  const lista = [...visiveis()].sort((a, b) =>
    ORDEM_STATUS.indexOf(a.status) - ORDEM_STATUS.indexOf(b.status) || b.diasEmAberto - a.diasEmAberto);
  $("fila").innerHTML = htmlKpis() + (lista.length === 0
    ? '<div class="vazio">Nenhuma ocorrência' + (estado.filtro ? " com este status." : " registrada ainda.") + "</div>"
    : '<div class="lista">' + lista.map((o) => htmlCard(o, { comMapa: true })).join("") + "</div>");
}

function renderDetalhe() {
  const o = estado.ocorrencias.find((x) => x.id === estado.detalheId);
  if (!o) { $("detalhe").innerHTML = '<div class="vazio">Ocorrência não encontrada.</div>'; return; }

  const indice = ORDEM_STATUS.indexOf(o.status);
  const passos = ORDEM_STATUS.map((s, i) => {
    const marcador = i < indice ? "✓" : String(i + 1);
    const cor = i <= indice ? "cor-" + s : "";
    const classe = i < indice ? "feito" : i === indice ? "atual" : "futuro";
    const sub = i === 0 ? "<small>Registrada em " + esc(formatarDataHora(o.dataHoraRegistro)) + "</small>" : "";
    return '<li class="passo ' + classe + '"><span class="bolinha ' + cor + '">' + marcador + "</span><div><strong>" + ROTULO_STATUS[s] + "</strong>" + sub + "</div></li>";
  }).join("");

  let acoes;
  if (ehAdmin()) {
    const permitidos = o.proximosStatus.map((p) => p.valor);
    acoes = '<div class="acoes"><p class="nota">Ações do administrador</p>' +
      ["EM_ANALISE", "RESOLVIDO"].map((s) =>
        '<button type="button" class="acao" data-acao="status" data-id="' + esc(o.id) + '" data-status="' + s + '"' +
        (permitidos.includes(s) ? "" : " disabled") + ">" + ROTULO_ACAO[s] + "</button>").join("") + "</div>";
  } else {
    acoes = '<p class="nota-cliente">Somente o órgão responsável (administrador) altera o status. Você será avisado aqui quando houver novidades.</p>';
  }

  const fechado = o.status === "RESOLVIDO";
  const unidade = o.diasEmAberto === 1 ? "dia" : "dias";
  $("detalhe").innerHTML =
    '<div class="hero"><small>' + esc(o.tipoDescricao) + "</small><strong>" + esc(o.endereco) + "</strong>" +
      '<div class="dias' + (fechado ? " fechado" : "") + '"><span class="n">' + o.diasEmAberto + "</span><span>" +
      (fechado ? unidade + " até a resolução" : unidade + " em aberto") + "</span></div></div>" +
    (o.descricao ? '<div class="painel-caixa"><h3>Descrição</h3><p>' + esc(o.descricao) + "</p></div>" : "") +
    '<div class="painel-caixa"><h3>Ciclo de vida</h3><ol class="passos">' + passos + "</ol></div>" +
    '<p class="meta">' +
      (o.autorNome ? "Registrada por " + esc(o.autorNome) + ". " : "") +
      (o.responsavelNome ? "Atendida por " + esc(o.responsavelNome) + ". " : "") +
      "Coordenadas: " + o.latitude.toFixed(4) + ", " + o.longitude.toFixed(4) + "</p>" + acoes;
}

function renderTudo(ajustarMapa) {
  renderResumo();
  renderCartao();
  if (ehAdmin()) renderFila(); else renderMinhas();
  if (estado.detalheId) renderDetalhe();
  Mapa.desenhar(visiveis(), estado.selecionadaId, ajustarMapa);
}

async function carregarDados(ajustarMapa) {
  estado.ocorrencias = await api.listar();
  renderTudo(ajustarMapa);
}

/* ---------- Ações ---------- */
async function alterarStatus(id, status, botao) {
  if (botao) botao.disabled = true;
  try {
    await api.atualizarStatus(id, status);
    await carregarDados(false);
    avisar("Status atualizado.");
  } catch (erro) {
    avisar(erro.message, true);
    try { await carregarDados(false); } catch (_) { /* mantém a tela atual */ }
  }
}

/* ---------- Formulário de registro (cliente) ---------- */
function agoraParaCampo() {
  const d = new Date();
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
  return d.toISOString().slice(0, 16);
}

function montarTipos(tipos) {
  $("tipos").innerHTML = tipos.map((t, i) =>
    '<label class="opcao-tipo"><input type="radio" name="tipo" value="' + esc(t.valor) + '"' + (i === 0 ? " checked" : "") +
    "><span>" + esc(t.descricao) + "</span></label>").join("");
}

function mostrarErroForm(mensagem) {
  const el = $("erro-form");
  el.textContent = mensagem || "";
  el.hidden = !mensagem;
}

async function enviarFormulario(evento) {
  evento.preventDefault();
  mostrarErroForm("");
  const endereco = $("endereco").value.trim();
  const tipo = document.querySelector('input[name="tipo"]:checked');
  if (!tipo) { mostrarErroForm("Escolha o tipo do problema."); return; }
  if (!endereco) { mostrarErroForm("Informe o endereço da ocorrência."); $("endereco").focus(); return; }

  let dataHora = $("dataHora").value;
  if (dataHora && dataHora.length === 16) dataHora += ":00";

  const botao = $("enviar");
  botao.disabled = true;
  botao.textContent = "Localizando endereço…";
  try {
    const criada = await api.criar({
      tipo: tipo.value,
      endereco,
      descricao: $("descricao").value.trim() || null,
      dataHoraRegistro: dataHora || null,
    });
    $("form").reset();
    $("dataHora").value = agoraParaCampo();
    estado.selecionadaId = criada.id;
    estado.filtro = null;
    await carregarDados(false);
    abrirTela("mapa");
    Mapa.focar(criada.latitude, criada.longitude);
    avisar("Ocorrência registrada. Ela aparece como pendente até o administrador analisar.");
  } catch (erro) {
    mostrarErroForm(erro.message);
  } finally {
    botao.disabled = false;
    botao.textContent = "Registrar ocorrência";
  }
}

/* ---------- Eventos ---------- */
function ligarEventos() {
  document.querySelectorAll(".nav button").forEach((b) =>
    b.addEventListener("click", () => {
      if (b.dataset.tela === "registrar") { mostrarErroForm(""); $("dataHora").value = agoraParaCampo(); }
      abrirTela(b.dataset.tela);
    }));

  document.querySelectorAll("[data-voltar]").forEach((b) => b.addEventListener("click", voltar));
  $("sair").addEventListener("click", () => sair());
  $("form").addEventListener("submit", enviarFormulario);

  document.addEventListener("click", (e) => {
    const alvo = e.target.closest("[data-acao],[data-filtro],[data-ir]");
    if (!alvo || alvo.disabled) return;
    if (alvo.dataset.filtro !== undefined) { aplicarFiltro(alvo.dataset.filtro); return; }
    if (alvo.dataset.ir) { abrirTela(alvo.dataset.ir); return; }
    const id = alvo.dataset.id;
    if (alvo.dataset.acao === "detalhe") abrirDetalhe(id);
    else if (alvo.dataset.acao === "mapa") { abrirTela("mapa"); selecionar(id, true); }
    else if (alvo.dataset.acao === "status") alterarStatus(id, alvo.dataset.status, alvo);
  });
}

/** Chamado por auth.js depois do login (ou ao reabrir a página com uma sessão válida). */
async function iniciarApp(usuario) {
  estado.usuario = usuario;
  estado.filtro = null;
  estado.selecionadaId = null;
  estado.detalheId = null;

  $("acesso").hidden = true;
  $("app").hidden = false;
  $("usuario-nome").textContent = usuario.nome;
  const selo = $("usuario-perfil");
  selo.textContent = usuario.perfilDescricao;
  selo.className = "perfil perfil-" + usuario.perfil;
  document.querySelectorAll("[data-perfil]").forEach((el) => { el.hidden = el.dataset.perfil !== usuario.perfil; });

  if (!estado.mapaIniciado) { Mapa.iniciar((id) => selecionar(id, true)); estado.mapaIniciado = true; }
  if (!estado.eventosLigados) { ligarEventos(); estado.eventosLigados = true; }
  abrirTela("mapa");

  try {
    const [sistema] = await Promise.all([api.sistema(), !ehAdmin() ? api.tipos().then(montarTipos) : Promise.resolve()]);
    $("armazenamento").textContent = "Dados: " + sistema.armazenamento;
    if (!ehAdmin()) $("dataHora").value = agoraParaCampo();
    await carregarDados(true);
  } catch (erro) {
    avisar(erro.message, true);
    $("cartao").innerHTML = '<div class="vazio">' + esc(erro.message) + "</div>";
  }
}
