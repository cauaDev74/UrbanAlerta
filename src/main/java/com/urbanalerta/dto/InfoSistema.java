package com.urbanalerta.dto;

/** Resposta do GET /sistema (público): onde os dados ficam e se as contas de demonstração existem. */
public record InfoSistema(String armazenamento, boolean demonstracao) {
}
