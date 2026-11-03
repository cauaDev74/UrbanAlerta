package com.urbanalerta.service;

import com.urbanalerta.exception.AcessoNegadoException;
import com.urbanalerta.exception.OcorrenciaNaoEncontradaException;
import com.urbanalerta.exception.TransicaoInvalidaException;
import com.urbanalerta.model.Coordenadas;
import com.urbanalerta.model.Ocorrencia;
import com.urbanalerta.model.PerfilUsuario;
import com.urbanalerta.model.StatusOcorrencia;
import com.urbanalerta.model.TipoOcorrencia;
import com.urbanalerta.model.Usuario;
import com.urbanalerta.repository.OcorrenciaRepository;
import com.urbanalerta.support.RepositorioFalso;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OcorrenciaServiceTest {

    private final Usuario cliente = Usuario.novo("Ana", "ana@teste.com", "hash", PerfilUsuario.CLIENTE);
    private final Usuario admin = Usuario.novo("Admin", "admin@teste.com", "hash", PerfilUsuario.ADMIN);

    // Geocodificador falso: não faz consulta na internet.
    private final GeocodificacaoService geocodificadorFalso = endereco -> new Coordenadas(-11.30, -41.85);

    private OcorrenciaService novoServico(GeocodificacaoService geo) {
        return new OcorrenciaService(RepositorioFalso.de(OcorrenciaRepository.class), geo);
    }

    private NovaOcorrencia dadosBasicos() {
        return new NovaOcorrencia(TipoOcorrencia.BURACO, "  Av. Principal, 120  ", "  ", null, null, null);
    }

    @Test
    void clienteRegistraOcorrenciaPendenteComAutorEEnderecoGeocodificado() {
        OcorrenciaService servico = novoServico(geocodificadorFalso);

        Ocorrencia o = servico.criar(dadosBasicos(), cliente);

        assertEquals(StatusOcorrencia.PENDENTE, o.getStatus());
        assertEquals(cliente, o.getAutor());
        assertEquals("Av. Principal, 120", o.getEndereco());
        assertEquals(-11.30, o.getLatitude());
        assertNull(o.getDescricao());
        assertEquals(1, servico.listar().size());
    }

    @Test
    void usaCoordenadasInformadasSemGeocodificar() {
        OcorrenciaService servico = novoServico(endereco -> {
            throw new IllegalStateException("Não deveria geocodificar");
        });

        Ocorrencia o = servico.criar(new NovaOcorrencia(TipoOcorrencia.SEM_ILUMINACAO, "Rua X", null, null, -12.0, -40.0), cliente);

        assertEquals(-12.0, o.getLatitude());
        assertEquals(-40.0, o.getLongitude());
    }

    @Test
    void administradorNaoRegistraOcorrencia() {
        OcorrenciaService servico = novoServico(geocodificadorFalso);
        assertThrows(AcessoNegadoException.class, () -> servico.criar(dadosBasicos(), admin));
    }

    @Test
    void administradorPercorreOCicloCompletoERegistraOResponsavel() {
        OcorrenciaService servico = novoServico(geocodificadorFalso);
        Ocorrencia o = servico.criar(dadosBasicos(), cliente);

        Ocorrencia emAnalise = servico.atualizarStatus(o.getId(), StatusOcorrencia.EM_ANALISE, admin);
        assertEquals(StatusOcorrencia.EM_ANALISE, emAnalise.getStatus());
        assertEquals(admin, emAnalise.getResponsavel());

        assertEquals(StatusOcorrencia.RESOLVIDO, servico.atualizarStatus(o.getId(), StatusOcorrencia.RESOLVIDO, admin).getStatus());
    }

    @Test
    void clienteNaoMudaStatus() {
        OcorrenciaService servico = novoServico(geocodificadorFalso);
        Ocorrencia o = servico.criar(dadosBasicos(), cliente);

        assertThrows(AcessoNegadoException.class,
                () -> servico.atualizarStatus(o.getId(), StatusOcorrencia.EM_ANALISE, cliente));
        assertEquals(StatusOcorrencia.PENDENTE, servico.buscar(o.getId()).getStatus());
    }

    @Test
    void naoPermitePularDePendenteParaResolvido() {
        OcorrenciaService servico = novoServico(geocodificadorFalso);
        Ocorrencia o = servico.criar(dadosBasicos(), cliente);

        assertThrows(TransicaoInvalidaException.class,
                () -> servico.atualizarStatus(o.getId(), StatusOcorrencia.RESOLVIDO, admin));
    }

    @Test
    void buscarIdInexistenteLancaExcecao() {
        OcorrenciaService servico = novoServico(geocodificadorFalso);

        assertThrows(OcorrenciaNaoEncontradaException.class, () -> servico.buscar(UUID.randomUUID()));
    }
}
