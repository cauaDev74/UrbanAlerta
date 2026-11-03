package com.urbanalerta.config;

import com.urbanalerta.security.AutenticacaoInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Define quais caminhos exigem login. Ficam livres: /auth/login, /auth/cadastro, /sistema e a interface (arquivos estáticos).
 */
@Configuration
public class ConfiguracaoWeb implements WebMvcConfigurer {

    private final AutenticacaoInterceptor autenticacao;
    private final String[] origensPermitidas;

    public ConfiguracaoWeb(AutenticacaoInterceptor autenticacao,
                           @Value("${urbanalerta.cors.origens:}") String[] origensPermitidas) {
        this.autenticacao = autenticacao;
        this.origensPermitidas = origensPermitidas;
    }

    /**
     * CORS: só é ligado quando urbanalerta.cors.origens tem algum site (ex.: o front-end na Vercel).
     * Sem isso, o navegador bloqueia chamadas de um domínio para outro.
     */
    @Override
    public void addCorsMappings(CorsRegistry registro) {
        if (origensPermitidas.length == 0 || origensPermitidas[0].isBlank()) {
            return;
        }
        registro.addMapping("/**")
                .allowedOrigins(origensPermitidas)
                .allowedMethods("GET", "POST", "PATCH", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registro) {
        registro.addInterceptor(autenticacao)
                .addPathPatterns("/ocorrencias", "/ocorrencias/**", "/tipos-ocorrencia", "/auth/eu");
    }
}
