package com.urbanalerta.dto;

import com.urbanalerta.model.Usuario;

/** Dados públicos do usuário logado (nunca inclui a senha nem o hash). */
public record UsuarioResponse(String id, String nome, String email, String perfil, String perfilDescricao) {

    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId().toString(), u.getNome(), u.getEmail(),
                u.getPerfil().name(), u.getPerfil().getDescricao());
    }
}
