package com.urbanalerta.exception;

import com.urbanalerta.model.StatusOcorrencia;

public class TransicaoInvalidaException extends RuntimeException {
    public TransicaoInvalidaException(StatusOcorrencia de, StatusOcorrencia para) {
        super("Transição de status inválida: " + de.getDescricao() + " para " + para.getDescricao()
                + ". O ciclo é Pendente, Em análise e Resolvido, nessa ordem.");
    }
}
