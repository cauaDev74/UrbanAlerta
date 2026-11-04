package com.urbanalerta.config;

import com.urbanalerta.model.PerfilUsuario;
import com.urbanalerta.repository.UsuarioRepository;
import com.urbanalerta.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Cria o administrador na primeira inicialização quando suas credenciais estão configuradas.
 */
@Component
public class InicializadorDeDados implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(InicializadorDeDados.class);

    private final UsuarioRepository usuarios;
    private final AuthService auth;
    private final String adminNome;
    private final String adminEmail;
    private final String adminSenha;

    public InicializadorDeDados(UsuarioRepository usuarios, AuthService auth,
                                @Value("${urbanalerta.admin.nome:Administrador}") String adminNome,
                                @Value("${urbanalerta.admin.email:}") String adminEmail,
                                @Value("${urbanalerta.admin.senha:}") String adminSenha) {
        this.usuarios = usuarios;
        this.auth = auth;
        this.adminNome = adminNome;
        this.adminEmail = adminEmail.trim().toLowerCase(Locale.ROOT);
        this.adminSenha = adminSenha;
    }

    @Override
    public void run(String... args) {
        criarAdministrador();
    }

    private void criarAdministrador() {
        if (adminSenha == null || adminSenha.isBlank() || adminEmail.isBlank()) {
            log.warn("Nenhum administrador foi criado: defina ADMIN_EMAIL e ADMIN_SENHA.");
            return;
        }
        if (!usuarios.existsByEmail(adminEmail)) {
            auth.criarUsuario(adminNome, adminEmail, adminSenha, PerfilUsuario.ADMIN);
            log.info("Administrador criado: {}", adminEmail);
        }
    }

}
