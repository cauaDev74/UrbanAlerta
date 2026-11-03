/**
 * mapa.js
 * Encapsula o mapa Leaflet (OpenStreetMap): marcadores coloridos por status.
 */
const Mapa = (() => {
  const CLASSE_COR = { PENDENTE: "pino-ambar", EM_ANALISE: "pino-azul", RESOLVIDO: "pino-ok" };
  const CENTRO_PADRAO = [-11.3033, -41.8533]; // Irecê, BA
  let mapa;
  let camada;
  let aoSelecionar = () => {};

  function iniciar(callback) {
    aoSelecionar = callback;
    mapa = L.map("mapa").setView(CENTRO_PADRAO, 14);
    L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 19,
      attribution: "&copy; colaboradores do OpenStreetMap",
    }).addTo(mapa);
    camada = L.layerGroup().addTo(mapa);
  }

  function icone(status, selecionado) {
    return L.divIcon({
      className: "pino",
      html: '<span class="pino-bolha ' + CLASSE_COR[status] + (selecionado ? " sel" : "") + '"></span>',
      iconSize: [24, 24],
      iconAnchor: [12, 12],
    });
  }

  function desenhar(ocorrencias, selecionadaId, ajustar) {
    camada.clearLayers();
    const pontos = [];
    ocorrencias.forEach((o) => {
      const ponto = [o.latitude, o.longitude];
      pontos.push(ponto);
      L.marker(ponto, { icon: icone(o.status, o.id === selecionadaId), title: o.tipoDescricao + " - " + o.endereco })
        .on("click", () => aoSelecionar(o.id))
        .addTo(camada);
    });
    if (ajustar && pontos.length > 0) {
      mapa.fitBounds(L.latLngBounds(pontos).pad(0.25), { maxZoom: 16 });
    }
  }

  function focar(latitude, longitude) {
    mapa.invalidateSize(); // garante o tamanho correto se o mapa acabou de ficar visível
    mapa.setView([latitude, longitude], 16);
  }

  /** Necessário quando o mapa volta a ficar visível depois de estar escondido. */
  function redimensionar() {
    setTimeout(() => mapa.invalidateSize(), 60);
  }

  return { iniciar, desenhar, focar, redimensionar };
})();
