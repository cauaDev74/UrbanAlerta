package com.urbanalerta.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusOcorrenciaTest {

    @Test
    void cicloAvancaUmPassoPorVez() {
        assertTrue(StatusOcorrencia.PENDENTE.podeTransicionarPara(StatusOcorrencia.EM_ANALISE));
        assertTrue(StatusOcorrencia.EM_ANALISE.podeTransicionarPara(StatusOcorrencia.RESOLVIDO));
    }

    @Test
    void naoPermitePularEtapaNemVoltar() {
        assertFalse(StatusOcorrencia.PENDENTE.podeTransicionarPara(StatusOcorrencia.RESOLVIDO));
        assertFalse(StatusOcorrencia.EM_ANALISE.podeTransicionarPara(StatusOcorrencia.PENDENTE));
        assertFalse(StatusOcorrencia.PENDENTE.podeTransicionarPara(StatusOcorrencia.PENDENTE));
    }

    @Test
    void resolvidoEhStatusFinal() {
        assertEquals(0, StatusOcorrencia.RESOLVIDO.proximosPermitidos().size());
    }
}
