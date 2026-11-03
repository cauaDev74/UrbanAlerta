package com.urbanalerta.dto;

import com.urbanalerta.service.Sessao;

/** Resposta do login/cadastro: o token JWT e os dados do usuário. */
public record LoginResponse(String token, UsuarioResponse usuario) {

    public static LoginResponse de(Sessao sessao) {
        return new LoginResponse(sessao.token(), UsuarioResponse.de(sessao.usuario()));
    }
}
