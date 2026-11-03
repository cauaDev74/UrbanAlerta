package com.urbanalerta.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SenhaServiceTest {

    private final SenhaService senhas = new SenhaService();

    @Test
    void hashNaoContemASenhaEConfereCorretamente() {
        String hash = senhas.gerarHash("minha-senha");
        assertFalse(hash.contains("minha-senha"));
        assertTrue(senhas.conferir("minha-senha", hash));
    }

    @Test
    void senhaErradaNaoConfere() {
        String hash = senhas.gerarHash("minha-senha");
        assertFalse(senhas.conferir("outra-senha", hash));
    }

    @Test
    void mesmaSenhaGeraHashesDiferentesPorCausaDoSal() {
        assertNotEquals(senhas.gerarHash("igual"), senhas.gerarHash("igual"));
    }

    @Test
    void hashEmFormatoInvalidoNuncaConfere() {
        assertFalse(senhas.conferir("qualquer", "texto-sem-formato"));
        assertFalse(senhas.conferir("qualquer", "pbkdf2$abc$@@@$@@@"));
    }
}
