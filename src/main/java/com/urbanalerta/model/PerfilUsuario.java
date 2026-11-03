package com.urbanalerta.model;

/** Papel do usuário no sistema: o cliente registra ocorrências, o administrador as resolve. */
public enum PerfilUsuario {
    CLIENTE("Cliente"),
    ADMIN("Administrador");

    private final String descricao;

    PerfilUsuario(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
