package com.urbanalerta.dto;

/** Formato padrão das mensagens de erro da API: {"erro": "..."}. */
public record ErroResponse(String erro) {
}
