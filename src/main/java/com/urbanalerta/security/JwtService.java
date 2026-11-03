package com.urbanalerta.security;

import com.urbanalerta.model.PerfilUsuario;
import com.urbanalerta.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gera e valida tokens JWT (HS256), usando só a biblioteca padrão do Java.
 * Um JWT tem 3 partes em Base64URL separadas por ponto: cabeçalho.dados.assinatura.
 * A assinatura (HMAC-SHA256 com o segredo do servidor) impede que alguém altere o perfil ou o prazo.
 * Em um sistema de produção, prefira o Spring Security; aqui a implementação é pequena de propósito.
 */
@Component
public class JwtService {

    private static final String CABECALHO = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
    private static final Pattern SUB = Pattern.compile("\"sub\":\"([0-9a-fA-F-]{36})\"");
    private static final Pattern PERFIL = Pattern.compile("\"perfil\":\"([A-Z_]+)\"");
    private static final Pattern EXP = Pattern.compile("\"exp\":(\\d+)");

    private final byte[] segredo;
    private final long validadeSegundos;

    public JwtService(@Value("${urbanalerta.seguranca.jwt-segredo}") String segredo,
                      @Value("${urbanalerta.seguranca.jwt-horas:8}") long horas) {
        if (segredo == null || segredo.length() < 32) {
            throw new IllegalStateException("urbanalerta.seguranca.jwt-segredo precisa ter pelo menos 32 caracteres.");
        }
        this.segredo = segredo.getBytes(StandardCharsets.UTF_8);
        this.validadeSegundos = horas * 3600;
    }

    /** Dados que o servidor lê de volta de um token válido. */
    public record DadosToken(UUID usuarioId, PerfilUsuario perfil) {
    }

    public String gerarToken(Usuario usuario) {
        return gerarToken(usuario, Instant.now());
    }

    public String gerarToken(Usuario usuario, Instant agora) {
        long expira = agora.plusSeconds(validadeSegundos).getEpochSecond();
        String dados = "{\"sub\":\"" + usuario.getId() + "\",\"perfil\":\"" + usuario.getPerfil().name()
                + "\",\"iat\":" + agora.getEpochSecond() + ",\"exp\":" + expira + "}";
        String corpo = CABECALHO + "." + base64Url(dados.getBytes(StandardCharsets.UTF_8));
        return corpo + "." + base64Url(assinar(corpo));
    }

    public Optional<DadosToken> validar(String token) {
        return validar(token, Instant.now());
    }

    /** Devolve os dados do token, ou vazio se a assinatura, o formato ou o prazo estiverem inválidos. */
    public Optional<DadosToken> validar(String token, Instant agora) {
        try {
            if (token == null) {
                return Optional.empty();
            }
            String[] partes = token.split("\\.");
            if (partes.length != 3) {
                return Optional.empty();
            }
            // A assinatura é sempre conferida, qualquer que seja o cabeçalho enviado (nada de "alg: none").
            byte[] esperada = assinar(partes[0] + "." + partes[1]);
            byte[] recebida = Base64.getUrlDecoder().decode(partes[2]);
            if (!MessageDigest.isEqual(esperada, recebida)) {
                return Optional.empty();
            }
            String dados = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
            Matcher sub = SUB.matcher(dados);
            Matcher perfil = PERFIL.matcher(dados);
            Matcher exp = EXP.matcher(dados);
            if (!sub.find() || !perfil.find() || !exp.find()) {
                return Optional.empty();
            }
            if (Long.parseLong(exp.group(1)) <= agora.getEpochSecond()) {
                return Optional.empty();
            }
            return Optional.of(new DadosToken(UUID.fromString(sub.group(1)), PerfilUsuario.valueOf(perfil.group(1))));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private byte[] assinar(String texto) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(segredo, "HmacSHA256"));
            return mac.doFinal(texto.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 indisponível neste Java.", e);
        }
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
