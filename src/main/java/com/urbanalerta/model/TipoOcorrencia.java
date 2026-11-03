package com.urbanalerta.model;

/** Tipos de problema de infraestrutura urbana que podem ser reportados. */
public enum TipoOcorrencia {
    BURACO("Buraco na via"),
    SEM_ILUMINACAO("Falta de iluminação"),
    ALTO_INDICE_ACIDENTES("Alto índice de acidentes"),
    LIXO_ACUMULADO("Lixo acumulado"),
    ESGOTO_A_CEU_ABERTO("Esgoto a céu aberto"),
    SEMAFORO_QUEBRADO("Semáforo quebrado");

    private final String descricao;

    TipoOcorrencia(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
