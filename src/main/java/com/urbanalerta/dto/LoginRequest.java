package com.urbanalerta.dto;

import jakarta.validation.constraints.NotBlank;

/** Corpo do POST /auth/login. */
public record LoginRequest(
        @NotBlank(message = "Informe seu e-mail.") String email,
        @NotBlank(message = "Informe sua senha.") String senha) {
}
