package com.urbanalerta.exception;

/** O usuário está logado, mas o perfil dele não permite a ação (HTTP 403). */
public class AcessoNegadoException extends RuntimeException {
    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
