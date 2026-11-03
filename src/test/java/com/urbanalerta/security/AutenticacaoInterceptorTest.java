package com.urbanalerta.security;

import com.urbanalerta.exception.AcessoNegadoException;
import com.urbanalerta.exception.NaoAutenticadoException;
import com.urbanalerta.model.PerfilUsuario;
import com.urbanalerta.model.Usuario;
import com.urbanalerta.repository.UsuarioRepository;
import com.urbanalerta.service.AuthService;
import com.urbanalerta.support.RepositorioFalso;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutenticacaoInterceptorTest {

    /** Controller de mentira com um endpoint livre e um só para ADMIN. */
    public static class ControllerFalso {
        public void qualquerUsuario() { }

        @ExigePerfil(PerfilUsuario.ADMIN)
        public void soAdmin() { }
    }

    private final SenhaService senhas = new SenhaService();
    private final JwtService jwt = new JwtService("segredo-de-teste-com-mais-de-32-caracteres!", 8);
    private final UsuarioRepository usuarios = RepositorioFalso.de(UsuarioRepository.class);
    private final AuthService auth = new AuthService(usuarios, senhas, jwt);
    private final AutenticacaoInterceptor interceptor = new AutenticacaoInterceptor(jwt, auth);

    private HandlerMethod endpoint(String nome) throws NoSuchMethodException {
        Method metodo = ControllerFalso.class.getMethod(nome);
        return new HandlerMethod(new ControllerFalso(), metodo);
    }

    private MockHttpServletRequest comToken(String token) {
        MockHttpServletRequest requisicao = new MockHttpServletRequest();
        requisicao.addHeader("Authorization", "Bearer " + token);
        return requisicao;
    }

    @Test
    void semCabecalhoEhNaoAutenticado() throws Exception {
        HandlerMethod livre = endpoint("qualquerUsuario");
        assertThrows(NaoAutenticadoException.class, () ->
                interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), livre));
    }

    @Test
    void consultaPreviaDoCorsPassaSemToken() throws Exception {
        MockHttpServletRequest requisicao = new MockHttpServletRequest();
        requisicao.setMethod("OPTIONS");
        assertTrue(interceptor.preHandle(requisicao, new MockHttpServletResponse(), endpoint("soAdmin")));
    }

    @Test
    void tokenInvalidoEhNaoAutenticado() throws Exception {
        HandlerMethod livre = endpoint("qualquerUsuario");
        assertThrows(NaoAutenticadoException.class, () ->
                interceptor.preHandle(comToken("lixo"), new MockHttpServletResponse(), livre));
    }

    @Test
    void clienteLogadoPassaEmEndpointLivreEFicaDisponivelNaRequisicao() throws Exception {
        Usuario cliente = auth.criarUsuario("Ana", "ana@teste.com", "123456", PerfilUsuario.CLIENTE);
        MockHttpServletRequest requisicao = comToken(jwt.gerarToken(cliente));

        assertTrue(interceptor.preHandle(requisicao, new MockHttpServletResponse(), endpoint("qualquerUsuario")));
        assertEquals(cliente, requisicao.getAttribute(AutenticacaoInterceptor.ATRIBUTO_USUARIO));
    }

    @Test
    void clienteNaoPassaEmEndpointDeAdmin() throws Exception {
        Usuario cliente = auth.criarUsuario("Ana", "ana@teste.com", "123456", PerfilUsuario.CLIENTE);
        HandlerMethod soAdmin = endpoint("soAdmin");
        assertThrows(AcessoNegadoException.class, () ->
                interceptor.preHandle(comToken(jwt.gerarToken(cliente)), new MockHttpServletResponse(), soAdmin));
    }

    @Test
    void adminPassaEmEndpointDeAdmin() throws Exception {
        Usuario admin = auth.criarUsuario("Admin", "admin@teste.com", "123456", PerfilUsuario.ADMIN);
        assertTrue(interceptor.preHandle(comToken(jwt.gerarToken(admin)), new MockHttpServletResponse(), endpoint("soAdmin")));
    }

    @Test
    void tokenDeUsuarioQueNaoExisteMaisEhRecusado() throws Exception {
        Usuario fantasma = Usuario.novo("Fantasma", "fantasma@teste.com", "hash", PerfilUsuario.ADMIN); // não salvo
        HandlerMethod soAdmin = endpoint("soAdmin");
        assertThrows(NaoAutenticadoException.class, () ->
                interceptor.preHandle(comToken(jwt.gerarToken(fantasma)), new MockHttpServletResponse(), soAdmin));
    }
}
