package com.urbanalerta.dto;

import com.urbanalerta.model.StatusOcorrencia;
import jakarta.validation.constraints.NotNull;

/** Corpo do PATCH /ocorrencias/{id}/status. */
public record AtualizarStatusRequest(
        @NotNull(message = "Informe o novo status.")
        StatusOcorrencia status) {
}
