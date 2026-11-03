package com.urbanalerta.exception;

public class GeocodificacaoIndisponivelException extends RuntimeException {
    public GeocodificacaoIndisponivelException(String mensagem) {
        super(mensagem);
    }

    public GeocodificacaoIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
