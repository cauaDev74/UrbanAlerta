package com.urbanalerta.exception;

public class EnderecoNaoEncontradoException extends RuntimeException {
    public EnderecoNaoEncontradoException(String endereco) {
        super("Endereço não encontrado: \"" + endereco + "\". Tente incluir cidade e estado, por exemplo: Rua X, 123, Irecê, BA.");
    }
}
