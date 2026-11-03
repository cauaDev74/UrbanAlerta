package com.urbanalerta.dto;

import com.urbanalerta.model.TipoOcorrencia;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** Corpo do POST /ocorrencias. Data e coordenadas são opcionais. */
public record NovaOcorrenciaRequest(
        @NotNull(message = "Informe o tipo da ocorrência.")
        TipoOcorrencia tipo,

        @NotBlank(message = "Informe o endereço da ocorrência.")
        @Size(max = 300, message = "O endereço deve ter no máximo 300 caracteres.")
        String endereco,

        @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres.")
        String descricao,

        LocalDateTime dataHoraRegistro,

        @DecimalMin(value = "-90.0", message = "Latitude inválida.")
        @DecimalMax(value = "90.0", message = "Latitude inválida.")
        Double latitude,

        @DecimalMin(value = "-180.0", message = "Longitude inválida.")
        @DecimalMax(value = "180.0", message = "Longitude inválida.")
        Double longitude) {
}
