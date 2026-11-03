package com.urbanalerta.security;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Guarda senhas com PBKDF2-HMAC-SHA256 (biblioteca padrão do Java): cada senha tem um "sal" aleatório
 * e passa por muitas iterações, o que torna ataques de força bruta caros.
 * Formato guardado no banco: pbkdf2$iterações$sal$hash (tudo em Base64).
 */
@Component
public class SenhaService {

    private static final int ITERACOES = 210_000;
    private static final int TAMANHO_SAL_BYTES = 16;
    private static final int TAMANHO_HASH_BITS = 256;

    private final SecureRandom aleatorio = new SecureRandom();

    public String gerarHash(String senha) {
        byte[] sal = new byte[TAMANHO_SAL_BYTES];
        aleatorio.nextBytes(sal);
        byte[] hash = pbkdf2(senha, sal, ITERACOES, TAMANHO_HASH_BITS);
        return "pbkdf2$" + ITERACOES + "$" + codificar(sal) + "$" + codificar(hash);
    }

    public boolean conferir(String senha, String armazenado) {
        try {
            String[] partes = armazenado.split("\\$");
            if (partes.length != 4 || !partes[0].equals("pbkdf2")) {
                return false;
            }
            int iteracoes = Integer.parseInt(partes[1]);
            byte[] sal = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            byte[] calculado = pbkdf2(senha, sal, iteracoes, esperado.length * 8);
            return MessageDigest.isEqual(esperado, calculado); // comparação em tempo constante
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static byte[] pbkdf2(String senha, byte[] sal, int iteracoes, int bits) {
        try {
            PBEKeySpec especificacao = new PBEKeySpec(senha.toCharArray(), sal, iteracoes, bits);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(especificacao).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 indisponível neste Java.", e);
        }
    }

    private static String codificar(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}
