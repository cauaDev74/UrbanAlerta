package com.urbanalerta.model;

import java.util.List;

/**
 * Ciclo de vida de uma ocorrência: PENDENTE -> EM_ANALISE -> RESOLVIDO.
 * A regra de transição fica no modelo, e não na tela nem no controller.
 */
public enum StatusOcorrencia {
    PENDENTE("Pendente"),
    EM_ANALISE("Em análise"),
    RESOLVIDO("Resolvido");

    private final String descricao;

    StatusOcorrencia(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Status para os quais é permitido avançar a partir deste. */
    public List<StatusOcorrencia> proximosPermitidos() {
        return switch (this) {
            case PENDENTE -> List.of(EM_ANALISE);
            case EM_ANALISE -> List.of(RESOLVIDO);
            case RESOLVIDO -> List.of(); // status final
        };
    }

    public boolean podeTransicionarPara(StatusOcorrencia novo) {
        return proximosPermitidos().contains(novo);
    }
}
