package com.urbanalerta.controller;

import com.urbanalerta.dto.AtualizarStatusRequest;
import com.urbanalerta.dto.NovaOcorrenciaRequest;
import com.urbanalerta.dto.OcorrenciaResponse;
import com.urbanalerta.dto.Opcao;
import com.urbanalerta.model.PerfilUsuario;
import com.urbanalerta.model.TipoOcorrencia;
import com.urbanalerta.model.Usuario;
import com.urbanalerta.security.AutenticacaoInterceptor;
import com.urbanalerta.security.ExigePerfil;
import com.urbanalerta.service.NovaOcorrencia;
import com.urbanalerta.service.OcorrenciaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Camada de controle das ocorrências. Todos os endpoints exigem login.
 * Cliente: registra e consulta. Administrador: consulta e muda o status.
 * O controller só traduz HTTP para chamadas ao serviço.
 */
@RestController
public class OcorrenciaController {

    private static final String USUARIO = AutenticacaoInterceptor.ATRIBUTO_USUARIO;

    private final OcorrenciaService servico;

    public OcorrenciaController(OcorrenciaService servico) {
        this.servico = servico;
    }

    /** Lista todas as ocorrências (popula o mapa). */
    @GetMapping("/ocorrencias")
    public List<OcorrenciaResponse> listar(@RequestAttribute(USUARIO) Usuario usuario) {
        return servico.listar().stream().map(o -> OcorrenciaResponse.de(o, usuario)).toList();
    }

    /** Detalhe de uma ocorrência. */
    @GetMapping("/ocorrencias/{id}")
    public OcorrenciaResponse buscar(@PathVariable UUID id, @RequestAttribute(USUARIO) Usuario usuario) {
        return OcorrenciaResponse.de(servico.buscar(id), usuario);
    }

    /** Somente CLIENTE. Sem coordenadas, o servidor geocodifica o endereço. */
    @PostMapping("/ocorrencias")
    @ResponseStatus(HttpStatus.CREATED)
    @ExigePerfil(PerfilUsuario.CLIENTE)
    public OcorrenciaResponse criar(@Valid @RequestBody NovaOcorrenciaRequest req,
                                    @RequestAttribute(USUARIO) Usuario usuario) {
        return OcorrenciaResponse.de(servico.criar(new NovaOcorrencia(
                req.tipo(), req.endereco(), req.descricao(),
                req.dataHoraRegistro(), req.latitude(), req.longitude()), usuario), usuario);
    }

    /** Somente ADMIN. Respeita a ordem do ciclo: Pendente, Em análise, Resolvido. */
    @PatchMapping("/ocorrencias/{id}/status")
    @ExigePerfil(PerfilUsuario.ADMIN)
    public OcorrenciaResponse atualizarStatus(@PathVariable UUID id,
                                              @Valid @RequestBody AtualizarStatusRequest req,
                                              @RequestAttribute(USUARIO) Usuario usuario) {
        return OcorrenciaResponse.de(servico.atualizarStatus(id, req.status(), usuario), usuario);
    }

    /** Tipos de ocorrência disponíveis (usado pelo formulário). */
    @GetMapping("/tipos-ocorrencia")
    public List<Opcao> tipos() {
        return Arrays.stream(TipoOcorrencia.values())
                .map(t -> new Opcao(t.name(), t.getDescricao()))
                .toList();
    }
}
