package com.urbanalerta.dto;

import com.urbanalerta.model.Ocorrencia;
import com.urbanalerta.model.PerfilUsuario;
import com.urbanalerta.model.Usuario;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Como uma ocorrência é devolvida pela API. O conteúdo depende de quem está olhando:
 *  - proximosStatus só vem preenchido para o administrador (é ele quem pode agir);
 *  - o nome do autor só aparece para o administrador e para o próprio autor.
 */
public record OcorrenciaResponse(
        String id,
        String tipo,
        String tipoDescricao,
        String endereco,
        double latitude,
        double longitude,
        LocalDateTime dataHoraRegistro,
        String descricao,
        String status,
        String statusDescricao,
        long diasEmAberto,
        LocalDateTime atualizadoEm,
        boolean minha,
        String autorNome,
        String responsavelNome,
        List<Opcao> proximosStatus) {

    public static OcorrenciaResponse de(Ocorrencia o, Usuario visualizador) {
        boolean admin = visualizador.getPerfil() == PerfilUsuario.ADMIN;
        Usuario autor = o.getAutor();
        boolean minha = autor != null && autor.getId().equals(visualizador.getId());
        List<Opcao> proximos = admin
                ? o.getStatus().proximosPermitidos().stream().map(s -> new Opcao(s.name(), s.getDescricao())).toList()
                : List.of();
        return new OcorrenciaResponse(
                o.getId().toString(),
                o.getTipo().name(),
                o.getTipo().getDescricao(),
                o.getEndereco(),
                o.getLatitude(),
                o.getLongitude(),
                o.getDataHoraRegistro(),
                o.getDescricao(),
                o.getStatus().name(),
                o.getStatus().getDescricao(),
                o.diasEmAberto(),
                o.getAtualizadoEm(),
                minha,
                (autor != null && (admin || minha)) ? autor.getNome() : null,
                o.getResponsavel() != null ? o.getResponsavel().getNome() : null,
                proximos);
    }
}
