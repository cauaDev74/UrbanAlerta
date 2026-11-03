package com.urbanalerta.exception;

/** E-mail ou senha incorretos (HTTP 401). A mensagem não diz qual dos dois errou, de propósito. */
public class CredenciaisInvalidasException extends RuntimeException {
    public CredenciaisInvalidasException() {
        super("E-mail ou senha incorretos.");
    }
}
