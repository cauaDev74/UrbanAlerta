package com.urbanalerta.service;

import com.urbanalerta.model.TipoOcorrencia;

import java.time.LocalDateTime;

/**
 * Dados necessários para registrar uma ocorrência.
 * dataHoraRegistro, latitude e longitude são opcionais:
 * sem data usa-se o momento atual; sem coordenadas o endereço é geocodificado.
 */
public record NovaOcorrencia(TipoOcorrencia tipo, String endereco, String descricao,
                             LocalDateTime dataHoraRegistro, Double latitude, Double longitude) {
}
