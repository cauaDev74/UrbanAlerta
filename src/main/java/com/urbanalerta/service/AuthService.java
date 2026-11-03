package com.urbanalerta.service;

import com.urbanalerta.exception.CredenciaisInvalidasException;
import com.urbanalerta.exception.EmailJaCadastradoException;
import com.urbanalerta.exception.NaoAutenticadoException;
import com.urbanalerta.model.PerfilUsuario;
import com.urbanalerta.model.Usuario;
import com.urbanalerta.repository.UsuarioRepository;
import com.urbanalerta.security.JwtService;
import com.urbanalerta.security.SenhaService;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

/** Cadastro e login de usuários. */
@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final SenhaService senhas;
    private final JwtService jwt;
    /** Hash de uma senha qualquer: usado para gastar o mesmo tempo quando o e-mail não existe. */
    private final String hashFalso;

    public AuthService(UsuarioRepository usuarios, SenhaService senhas, JwtService jwt) {
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.jwt = jwt;
        this.hashFalso = senhas.gerarHash("senha-que-nao-existe");
    }

    /** Autocadastro pela tela: o perfil é sempre CLIENTE (administradores não se cadastram sozinhos). */
    public Sessao cadastrarCliente(String nome, String email, String senha) {
        Usuario usuario = criarUsuario(nome, email, senha, PerfilUsuario.CLIENTE);
        return new Sessao(jwt.gerarToken(usuario), usuario);
    }

    /** Cria um usuário com o perfil indicado (também usado para criar o administrador inicial). */
    public Usuario criarUsuario(String nome, String email, String senha, PerfilUsuario perfil) {
        String emailNormalizado = normalizar(email);
        if (usuarios.existsByEmail(emailNormalizado)) {
            throw new EmailJaCadastradoException();
        }
        return usuarios.save(Usuario.novo(nome.trim(), emailNormalizado, senhas.gerarHash(senha), perfil));
    }

    public Sessao entrar(String email, String senha) {
        Usuario usuario = usuarios.findByEmail(normalizar(email)).orElse(null);
        if (usuario == null) {
            senhas.conferir(senha, hashFalso); // gasta o mesmo tempo, para não revelar se o e-mail existe
            throw new CredenciaisInvalidasException();
        }
        if (!senhas.conferir(senha, usuario.getSenhaHash())) {
            throw new CredenciaisInvalidasException();
        }
        return new Sessao(jwt.gerarToken(usuario), usuario);
    }

    public Usuario buscarPorId(UUID id) {
        return usuarios.findById(id)
                .orElseThrow(() -> new NaoAutenticadoException("Usuário não encontrado. Faça login novamente."));
    }

    private static String normalizar(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
