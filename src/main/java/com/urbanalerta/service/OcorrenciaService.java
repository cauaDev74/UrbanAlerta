package com.urbanalerta.service;

import com.urbanalerta.exception.AcessoNegadoException;
import com.urbanalerta.exception.OcorrenciaNaoEncontradaException;
import com.urbanalerta.model.Coordenadas;
import com.urbanalerta.model.Ocorrencia;
import com.urbanalerta.model.PerfilUsuario;
import com.urbanalerta.model.StatusOcorrencia;
import com.urbanalerta.model.Usuario;
import com.urbanalerta.repository.OcorrenciaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Camada de serviço: casos de uso do sistema.
 * O cliente registra; o administrador muda o status. O controller já barra o perfil errado,
 * e o serviço confere de novo (defesa em profundidade).
 */
@Service
public class OcorrenciaService {

    private final OcorrenciaRepository ocorrencias;
    private final GeocodificacaoService geocodificacao;

    public OcorrenciaService(OcorrenciaRepository ocorrencias, GeocodificacaoService geocodificacao) {
        this.ocorrencias = ocorrencias;
        this.geocodificacao = geocodificacao;
    }

    public Ocorrencia criar(NovaOcorrencia dados, Usuario autor) {
        if (autor.getPerfil() != PerfilUsuario.CLIENTE) {
            throw new AcessoNegadoException("Somente clientes podem registrar ocorrências.");
        }
        String endereco = dados.endereco().trim();

        Coordenadas coordenadas = (dados.latitude() != null && dados.longitude() != null)
                ? new Coordenadas(dados.latitude(), dados.longitude())
                : geocodificacao.geocodificar(endereco);

        LocalDateTime registro = dados.dataHoraRegistro() != null ? dados.dataHoraRegistro() : LocalDateTime.now();
        String descricao = (dados.descricao() == null || dados.descricao().isBlank()) ? null : dados.descricao().trim();

        return ocorrencias.save(Ocorrencia.nova(dados.tipo(), endereco,
                coordenadas.latitude(), coordenadas.longitude(), registro, descricao, autor));
    }

    public List<Ocorrencia> listar() {
        return ocorrencias.findAllByOrderByDataHoraRegistroDesc();
    }

    public Ocorrencia buscar(UUID id) {
        return ocorrencias.findById(id).orElseThrow(() -> new OcorrenciaNaoEncontradaException(id));
    }

    /**
     * synchronized + lock otimista (@Version): duas pessoas mudando a mesma ocorrência ao mesmo tempo
     * não se sobrescrevem. A regra do ciclo de status está em Ocorrencia.mudarStatus.
     */
    public synchronized Ocorrencia atualizarStatus(UUID id, StatusOcorrencia novoStatus, Usuario responsavel) {
        if (responsavel.getPerfil() != PerfilUsuario.ADMIN) {
            throw new AcessoNegadoException("Somente o administrador pode alterar o status de uma ocorrência.");
        }
        Ocorrencia ocorrencia = buscar(id);
        ocorrencia.mudarStatus(novoStatus, responsavel);
        return ocorrencias.save(ocorrencia);
    }
}
