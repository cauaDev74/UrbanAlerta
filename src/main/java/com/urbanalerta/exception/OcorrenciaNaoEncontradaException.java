package com.urbanalerta.exception;

import java.util.UUID;

public class OcorrenciaNaoEncontradaException extends RuntimeException {
    public OcorrenciaNaoEncontradaException(UUID id) {
        super("Ocorrência não encontrada: " + id);
    }
}
