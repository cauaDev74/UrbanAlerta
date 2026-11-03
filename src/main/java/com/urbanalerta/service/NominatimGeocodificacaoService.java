package com.urbanalerta.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.urbanalerta.model.Coordenadas;
import com.urbanalerta.exception.EnderecoNaoEncontradoException;
import com.urbanalerta.exception.GeocodificacaoIndisponivelException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Geocodificação pelo Nominatim (OpenStreetMap), feita pelo servidor Java.
 * A consulta HTTP usa sockets TCP/IP do sistema operacional.
 * Política de uso do Nominatim: User-Agent identificável e no máximo 1 consulta por segundo,
 * por isso o método é synchronized e espera o intervalo mínimo entre consultas.
 */
@Service
public class NominatimGeocodificacaoService implements GeocodificacaoService {

    private static final long INTERVALO_MINIMO_MS = 1100;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper json = new ObjectMapper();
    private final String urlBase;
    private final String userAgent;
    private long ultimaConsultaMs = 0;

    public NominatimGeocodificacaoService(
            @Value("${urbanalerta.nominatim.url:https://nominatim.openstreetmap.org/search}") String urlBase,
            @Value("${urbanalerta.nominatim.user-agent:UrbanAlerta-Prototipo/0.2}") String userAgent) {
        this.urlBase = urlBase;
        this.userAgent = userAgent;
    }

    @Override
    public synchronized Coordenadas geocodificar(String endereco) {
        respeitarIntervalo();
        try {
            // countrycodes=br limita a busca ao Brasil, o que evita resultados de outros países.
            String url = urlBase + "?format=json&limit=1&countrycodes=br&q="
                    + URLEncoder.encode(endereco, StandardCharsets.UTF_8);

            HttpRequest requisicao = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", userAgent)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> resposta = http.send(requisicao, HttpResponse.BodyHandlers.ofString());
            ultimaConsultaMs = System.currentTimeMillis();

            if (resposta.statusCode() != 200) {
                throw new GeocodificacaoIndisponivelException(
                        "O serviço de geocodificação respondeu com o código " + resposta.statusCode() + ".");
            }

            JsonNode resultados = json.readTree(resposta.body());
            if (!resultados.isArray() || resultados.size() == 0) {
                throw new EnderecoNaoEncontradoException(endereco);
            }
            JsonNode primeiro = resultados.get(0);
            return new Coordenadas(primeiro.get("lat").asDouble(), primeiro.get("lon").asDouble());

        } catch (IOException e) {
            throw new GeocodificacaoIndisponivelException(
                    "Não foi possível consultar o serviço de geocodificação. Verifique a conexão com a internet.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GeocodificacaoIndisponivelException("A consulta de endereço foi interrompida.", e);
        }
    }

    private void respeitarIntervalo() {
        long espera = INTERVALO_MINIMO_MS - (System.currentTimeMillis() - ultimaConsultaMs);
        if (espera > 0) {
            try {
                Thread.sleep(espera);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
