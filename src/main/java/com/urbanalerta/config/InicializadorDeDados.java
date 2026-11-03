package com.urbanalerta.config;

import com.urbanalerta.model.Ocorrencia;
import com.urbanalerta.model.PerfilUsuario;
import com.urbanalerta.model.StatusOcorrencia;
import com.urbanalerta.model.TipoOcorrencia;
import com.urbanalerta.model.Usuario;
import com.urbanalerta.repository.OcorrenciaRepository;
import com.urbanalerta.repository.UsuarioRepository;
import com.urbanalerta.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

/**
 * Roda ao iniciar o sistema:
 *  1. cria o administrador, se a senha estiver configurada (urbanalerta.admin.senha) e ele ainda não existir;
 *  2. se urbanalerta.dados-demonstracao=true, cria dois clientes e 6 ocorrências FICTÍCIAS
 *     (endereços inventados, coordenadas aproximadas de Irecê, BA).
 */
@Component
public class InicializadorDeDados implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(InicializadorDeDados.class);

    private final UsuarioRepository usuarios;
    private final OcorrenciaRepository ocorrencias;
    private final AuthService auth;
    private final String adminNome;
    private final String adminEmail;
    private final String adminSenha;
    private final boolean demonstracao;

    public InicializadorDeDados(UsuarioRepository usuarios, OcorrenciaRepository ocorrencias, AuthService auth,
                                @Value("${urbanalerta.admin.nome:Administrador}") String adminNome,
                                @Value("${urbanalerta.admin.email:admin@urbanalerta.local}") String adminEmail,
                                @Value("${urbanalerta.admin.senha:}") String adminSenha,
                                @Value("${urbanalerta.dados-demonstracao:false}") boolean demonstracao) {
        this.usuarios = usuarios;
        this.ocorrencias = ocorrencias;
        this.auth = auth;
        this.adminNome = adminNome;
        this.adminEmail = adminEmail.trim().toLowerCase(Locale.ROOT);
        this.adminSenha = adminSenha;
        this.demonstracao = demonstracao;
    }

    @Override
    public void run(String... args) {
        criarAdministrador();
        if (demonstracao) {
            criarDemonstracao();
        }
    }

    private void criarAdministrador() {
        if (adminSenha == null || adminSenha.isBlank()) {
            log.warn("Nenhum administrador foi criado: defina urbanalerta.admin.senha (ou a variável ADMIN_SENHA).");
            return;
        }
        if (!usuarios.existsByEmail(adminEmail)) {
            auth.criarUsuario(adminNome, adminEmail, adminSenha, PerfilUsuario.ADMIN);
            log.info("Administrador criado: {}", adminEmail);
        }
    }

    private void criarDemonstracao() {
        if (ocorrencias.count() > 0) {
            return;
        }
        Usuario admin = usuarios.findByEmail(adminEmail).orElse(null);
        Usuario cliente = clienteDemo("Cliente Demonstração", "cliente@urbanalerta.local");
        Usuario vizinho = clienteDemo("Vizinho Demonstração", "vizinho@urbanalerta.local");

        adicionar(TipoOcorrencia.BURACO, "Av. Principal, 120, Irecê, BA", -11.3012, -41.8521,
                "Buraco grande na faixa da direita.", 4, StatusOcorrencia.PENDENTE, cliente, null);
        adicionar(TipoOcorrencia.SEM_ILUMINACAO, "Rua da Matriz, 45, Irecê, BA", -11.3059, -41.8566,
                "Três postes seguidos apagados.", 2, StatusOcorrencia.PENDENTE, cliente, null);
        adicionar(TipoOcorrencia.LIXO_ACUMULADO, "Praça Central, Irecê, BA", -11.3033, -41.8533,
                null, 6, StatusOcorrencia.PENDENTE, cliente, null);
        adicionar(TipoOcorrencia.SEMAFORO_QUEBRADO, "Cruzamento da Av. Principal com Rua das Flores, Irecê, BA",
                -11.3081, -41.8488, "Semáforo piscando em amarelo.", 3, StatusOcorrencia.EM_ANALISE, vizinho, admin);
        adicionar(TipoOcorrencia.ESGOTO_A_CEU_ABERTO, "Rua das Palmeiras, 300, Irecê, BA", -11.2987, -41.8579,
                null, 9, StatusOcorrencia.RESOLVIDO, vizinho, admin);
        adicionar(TipoOcorrencia.ALTO_INDICE_ACIDENTES, "Rua do Comércio, 210, Irecê, BA", -11.3045, -41.8601,
                "Esquina com baixa visibilidade.", 12, StatusOcorrencia.RESOLVIDO, vizinho, admin);
        log.info("Dados de demonstração criados (contas: cliente@urbanalerta.local / vizinho@urbanalerta.local).");
    }

    private Usuario clienteDemo(String nome, String email) {
        return usuarios.findByEmail(email).orElseGet(() -> auth.criarUsuario(nome, email, "cliente123", PerfilUsuario.CLIENTE));
    }

    private void adicionar(TipoOcorrencia tipo, String endereco, double latitude, double longitude, String descricao,
                           int diasAtras, StatusOcorrencia status, Usuario autor, Usuario responsavel) {
        LocalDateTime registro = LocalDateTime.now().minusDays(diasAtras);
        LocalDateTime atualizado = switch (status) {
            case PENDENTE -> registro;
            case EM_ANALISE -> registro.plusDays(1);
            case RESOLVIDO -> registro.plusDays(3);
        };
        ocorrencias.save(Ocorrencia.restaurar(UUID.randomUUID(), tipo, endereco, latitude, longitude,
                registro, descricao, status, atualizado, autor, responsavel));
    }
}
