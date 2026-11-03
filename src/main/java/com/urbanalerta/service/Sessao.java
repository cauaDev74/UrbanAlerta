package com.urbanalerta.service;

import com.urbanalerta.model.Usuario;

/** Resultado de um login/cadastro: o token JWT e o usuário autenticado. */
public record Sessao(String token, Usuario usuario) {
}
