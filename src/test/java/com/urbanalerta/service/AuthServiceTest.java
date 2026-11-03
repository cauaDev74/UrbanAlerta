package com.urbanalerta.service;

import com.urbanalerta.exception.CredenciaisInvalidasException;
import com.urbanalerta.exception.EmailJaCadastradoException;
import com.urbanalerta.model.PerfilUsuario;
import com.urbanalerta.model.Usuario;
import com.urbanalerta.repository.UsuarioRepository;
import com.urbanalerta.security.JwtService;
import com.urbanalerta.security.SenhaService;
import com.urbanalerta.support.RepositorioFalso;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    private final JwtService jwt = new JwtService("segredo-de-teste-com-mais-de-32-caracteres!", 8);
    private final AuthService auth = new AuthService(RepositorioFalso.de(UsuarioRepository.class), new SenhaService(), jwt);

    @Test
    void cadastroCriaClienteComSenhaEmHashEDevolveToken() {
        Sessao sessao = auth.cadastrarCliente("  Ana Souza ", "  ANA@Teste.com ", "senha123");

        Usuario u = sessao.usuario();
        assertEquals(PerfilUsuario.CLIENTE, u.getPerfil());
        assertEquals("Ana Souza", u.getNome());
        assertEquals("ana@teste.com", u.getEmail());
        assertNotEquals("senha123", u.getSenhaHash());
        assertTrue(jwt.validar(sessao.token()).isPresent());
    }

    @Test
    void emailDuplicadoEhRecusadoIgnorandoMaiusculas() {
        auth.cadastrarCliente("Ana", "ana@teste.com", "senha123");
        assertThrows(EmailJaCadastradoException.class, () -> auth.cadastrarCliente("Outra Ana", "ANA@TESTE.COM", "outra123"));
    }

    @Test
    void loginComSenhaCertaFunciona() {
        auth.cadastrarCliente("Ana", "ana@teste.com", "senha123");
        Sessao sessao = auth.entrar("Ana@Teste.com", "senha123");
        assertEquals("ana@teste.com", sessao.usuario().getEmail());
        assertFalse(sessao.token().isBlank());
    }

    @Test
    void loginComSenhaErradaOuEmailInexistenteTemAMesmaResposta() {
        auth.cadastrarCliente("Ana", "ana@teste.com", "senha123");
        CredenciaisInvalidasException senhaErrada =
                assertThrows(CredenciaisInvalidasException.class, () -> auth.entrar("ana@teste.com", "errada"));
        CredenciaisInvalidasException semConta =
                assertThrows(CredenciaisInvalidasException.class, () -> auth.entrar("ninguem@teste.com", "senha123"));
        assertEquals(senhaErrada.getMessage(), semConta.getMessage());
    }

    @Test
    void administradorSoExistePorCriacaoExplicita() {
        Usuario admin = auth.criarUsuario("Admin", "admin@teste.com", "admin123", PerfilUsuario.ADMIN);
        assertEquals(PerfilUsuario.ADMIN, admin.getPerfil());
        assertEquals(PerfilUsuario.CLIENTE, auth.cadastrarCliente("Ana", "ana@teste.com", "senha123").usuario().getPerfil());
    }
}
