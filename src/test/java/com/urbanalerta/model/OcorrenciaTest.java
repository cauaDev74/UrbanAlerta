package com.urbanalerta.model;

import com.urbanalerta.exception.TransicaoInvalidaException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OcorrenciaTest {

    private final Usuario cliente = Usuario.novo("Ana", "ana@teste.com", "hash", PerfilUsuario.CLIENTE);
    private final Usuario admin = Usuario.novo("Admin", "admin@teste.com", "hash", PerfilUsuario.ADMIN);

    @Test
    void diasEmAbertoContaDoRegistroAteAgora() {
        LocalDateTime agora = LocalDateTime.of(2026, 10, 14, 12, 0);
        Ocorrencia o = Ocorrencia.nova(TipoOcorrencia.BURACO, "Rua A", -11.3, -41.85, agora.minusDays(4), null, cliente);
        assertEquals(4, o.diasEmAberto(agora));
    }

    @Test
    void diasEmAbertoParaDeContarQuandoResolvida() {
        LocalDateTime agora = LocalDateTime.of(2026, 10, 14, 12, 0);
        Ocorrencia o = Ocorrencia.restaurar(UUID.randomUUID(), TipoOcorrencia.BURACO, "Rua A", -11.3, -41.85,
                agora.minusDays(10), null, StatusOcorrencia.RESOLVIDO, agora.minusDays(7), cliente, admin);
        assertEquals(3, o.diasEmAberto(agora));
    }

    @Test
    void ocorrenciaNovaComecaPendenteComAutorESemResponsavel() {
        Ocorrencia o = Ocorrencia.nova(TipoOcorrencia.LIXO_ACUMULADO, "Rua B", -11.3, -41.85, LocalDateTime.now(), null, cliente);
        assertEquals(StatusOcorrencia.PENDENTE, o.getStatus());
        assertEquals(cliente, o.getAutor());
        assertNull(o.getResponsavel());
    }

    @Test
    void mudarStatusRegistraOResponsavel() {
        Ocorrencia o = Ocorrencia.nova(TipoOcorrencia.BURACO, "Rua C", -11.3, -41.85, LocalDateTime.now(), null, cliente);
        o.mudarStatus(StatusOcorrencia.EM_ANALISE, admin);
        assertEquals(StatusOcorrencia.EM_ANALISE, o.getStatus());
        assertEquals(admin, o.getResponsavel());
    }

    @Test
    void naoPermitePularEtapa() {
        Ocorrencia o = Ocorrencia.nova(TipoOcorrencia.BURACO, "Rua D", -11.3, -41.85, LocalDateTime.now(), null, cliente);
        assertThrows(TransicaoInvalidaException.class, () -> o.mudarStatus(StatusOcorrencia.RESOLVIDO, admin));
    }
}
