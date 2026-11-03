package com.urbanalerta.service;

import com.urbanalerta.model.Coordenadas;

/** Converte um endereço em texto para latitude/longitude. */
@FunctionalInterface
public interface GeocodificacaoService {

    /**
     * @throws com.urbanalerta.exception.EnderecoNaoEncontradoException       se o endereço não existir
     * @throws com.urbanalerta.exception.GeocodificacaoIndisponivelException  se o serviço externo falhar
     */
    Coordenadas geocodificar(String endereco);
}
