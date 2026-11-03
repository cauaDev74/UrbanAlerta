package com.urbanalerta.security;

import com.urbanalerta.exception.AcessoNegadoException;
import com.urbanalerta.exception.NaoAutenticadoException;
import com.urbanalerta.model.Usuario;
import com.urbanalerta.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Roda antes de cada endpoint protegido: confere o token JWT do cabeçalho "Authorization: Bearer ...",
 * carrega o usuário do banco e, se o endpoint tiver @ExigePerfil, confere o perfil.
 * Os erros viram respostas 401/403 pelo TratadorDeErros.
 */
@Component
public class AutenticacaoInterceptor implements HandlerInterceptor {

    /** Nome do atributo da requisição onde o usuário autenticado fica disponível para os controllers. */
    public static final String ATRIBUTO_USUARIO = "usuarioAutenticado";

    private final JwtService jwt;
    private final AuthService auth;

    public AutenticacaoInterceptor(JwtService jwt, AuthService auth) {
        this.jwt = jwt;
        this.auth = auth;
    }

    @Override
    public boolean preHandle(HttpServletRequest requisicao, HttpServletResponse resposta, Object handler) {
        // O navegador faz uma consulta prévia (OPTIONS) do CORS sem o token: ela não pode ser barrada aqui.
        if ("OPTIONS".equalsIgnoreCase(requisicao.getMethod())) {
            return true;
        }
        String cabecalho = requisicao.getHeader("Authorization");
        if (cabecalho == null || !cabecalho.startsWith("Bearer ")) {
            throw new NaoAutenticadoException("Faça login para continuar.");
        }
        JwtService.DadosToken dados = jwt.validar(cabecalho.substring(7).trim())
                .orElseThrow(() -> new NaoAutenticadoException("Sessão inválida ou expirada. Faça login novamente."));

        // O perfil vem sempre do banco, não do token: se o perfil mudar, vale na hora.
        Usuario usuario = auth.buscarPorId(dados.usuarioId());

        if (handler instanceof HandlerMethod metodo) {
            ExigePerfil exigido = metodo.getMethodAnnotation(ExigePerfil.class);
            if (exigido != null && usuario.getPerfil() != exigido.value()) {
                throw new AcessoNegadoException("Seu perfil (" + usuario.getPerfil().getDescricao()
                        + ") não tem permissão para esta ação.");
            }
        }
        requisicao.setAttribute(ATRIBUTO_USUARIO, usuario);
        return true;
    }
}
