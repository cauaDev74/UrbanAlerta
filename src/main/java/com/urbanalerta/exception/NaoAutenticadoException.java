package com.urbanalerta.exception;

/** Falta login ou o token é inválido/expirado (HTTP 401). */
public class NaoAutenticadoException extends RuntimeException {
    public NaoAutenticadoException(String mensagem) {
        super(mensagem);
    }
}
