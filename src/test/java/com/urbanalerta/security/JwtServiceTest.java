package com.urbanalerta.security;

import com.urbanalerta.model.PerfilUsuario;
import com.urbanalerta.model.Usuario;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres!";
    private final JwtService jwt = new JwtService(SEGREDO, 8);
    private final Usuario admin = Usuario.novo("Admin", "admin@teste.com", "hash", PerfilUsuario.ADMIN);
    private final Instant agora = Instant.parse("2026-10-15T12:00:00Z");

    @Test
    void tokenValidoDevolveUsuarioEPerfil() {
        String token = jwt.gerarToken(admin, agora);
        Optional<JwtService.DadosToken> dados = jwt.validar(token, agora.plusSeconds(60));
        assertTrue(dados.isPresent());
        assertEquals(admin.getId(), dados.get().usuarioId());
        assertEquals(PerfilUsuario.ADMIN, dados.get().perfil());
    }

    @Test
    void tokenExpiradoEhRejeitado() {
        String token = jwt.gerarToken(admin, agora);
        assertTrue(jwt.validar(token, agora.plusSeconds(8 * 3600 + 1)).isEmpty());
    }

    @Test
    void tokenAdulteradoEhRejeitado() {
        String[] partes = jwt.gerarToken(admin, agora).split("\\.");
        // troca o perfil dentro dos dados, mantendo a assinatura antiga
        String dados = new String(Base64.getUrlDecoder().decode(partes[1]));
        String falso = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(dados.replace("ADMIN", "CLIENTE").getBytes());
        assertTrue(jwt.validar(partes[0] + "." + falso + "." + partes[2], agora).isEmpty());
    }

    @Test
    void tokenAssinadoComOutroSegredoEhRejeitado() {
        JwtService outro = new JwtService("outro-segredo-totalmente-diferente-123456", 8);
        assertTrue(jwt.validar(outro.gerarToken(admin, agora), agora).isEmpty());
    }

    @Test
    void lixoEhRejeitadoSemLancarErro() {
        assertTrue(jwt.validar(null, agora).isEmpty());
        assertTrue(jwt.validar("", agora).isEmpty());
        assertTrue(jwt.validar("a.b.c", agora).isEmpty());
        assertTrue(jwt.validar("so-um-pedaco", agora).isEmpty());
    }

    @Test
    void segredoCurtoEhRecusado() {
        assertThrows(IllegalStateException.class, () -> new JwtService("curto", 8));
    }
}
