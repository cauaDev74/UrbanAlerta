package com.urbanalerta.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FERRAMENTA DE DIAGNÓSTICO da conexão com o banco (não faz parte do sistema).
 * Como usar: no IntelliJ, clique com o botão direito neste arquivo e escolha "Run 'TesteConexao.main()'".
 * Rode a partir da pasta raiz do projeto (a que tem o pom.xml e o arquivo .env).
 *
 * Ela lê os mesmos valores que o perfil "banco" usa (arquivo .env e variáveis de ambiente), confere cada
 * item e tenta conectar de verdade, explicando em português o que deu errado. Nunca imprime a senha.
 */
public class TesteConexao {

    private static final Pattern MARCADOR = Pattern.compile("\\$\\{([A-Za-z0-9_.]+)(?::([^}]*))?}");

    private final PrintStream saida;
    private int erros = 0;

    TesteConexao(PrintStream saida) {
        this.saida = saida;
    }

    public static void main(String[] args) {
        int erros = new TesteConexao(new PrintStream(System.out, true, StandardCharsets.UTF_8)).executar(Path.of(".env"));
        System.exit(erros == 0 ? 0 : 1);
    }

    /** Roda todas as verificações e devolve quantos problemas graves encontrou. */
    int executar(Path arquivoEnv) {
        saida.println("=== Diagnóstico da conexão com o banco ===");
        saida.println("Pasta atual: " + Path.of("").toAbsolutePath());

        Properties padroes = new Properties();
        Properties env = new Properties();
        carregar(padroes, env, arquivoEnv);

        String dbUrl = valor(padroes, env, "DB_URL");
        String url = valor(padroes, env, "spring.datasource.url");
        String usuario = valor(padroes, env, "spring.datasource.username");
        String senha = valor(padroes, env, "spring.datasource.password");
        String jwt = valor(padroes, env, "urbanalerta.seguranca.jwt-segredo");
        String adminSenha = valor(padroes, env, "urbanalerta.admin.senha");

        conferirDbUrl(dbUrl, url);
        conferirPerfil(padroes, env);
        URI uri = conferirUrl(url);
        conferirUsuario(usuario, uri);
        conferirSenha(senha);
        conferirJwt(jwt);
        conferirAdmin(adminSenha);

        if (erros > 0 || uri == null) {
            saida.println();
            saida.println("PARE AQUI: corrija os itens [ERRO] acima e rode de novo.");
            return erros;
        }

        String host = uri.getHost();
        int porta = uri.getPort() == -1 ? 5432 : uri.getPort();
        if (conferirRede(host, porta)) {
            conferirBanco(url, usuario, senha);
        }

        saida.println();
        saida.println(erros == 0
                ? "RESULTADO: tudo certo. Pode rodar o sistema."
                : "RESULTADO: " + erros + " problema(s). Veja as dicas acima.");
        return erros;
    }

    // ---------------------------------------------------------------- leitura das configurações

    private void carregar(Properties padroes, Properties env, Path arquivoEnv) {
        try (InputStream in = TesteConexao.class.getResourceAsStream("/application-banco.properties")) {
            if (in != null) {
                padroes.load(in);
                ok("Achei application-banco.properties (as chaves que o perfil banco usa).");
            } else {
                erro("Não achei application-banco.properties dentro do projeto (src/main/resources).");
            }
        } catch (IOException e) {
            erro("Não consegui ler application-banco.properties (" + e.getClass().getSimpleName() + ").");
        }
        Path configExterna = Path.of("config", "application-banco.properties");
        if (Files.isRegularFile(configExterna)) {
            try (InputStream in = Files.newInputStream(configExterna)) {
                padroes.load(in);
                ok("Achei config/application-banco.properties (configuração externa do Spring).");
            } catch (IOException e) {
                erro("Não consegui ler config/application-banco.properties (" + e.getClass().getSimpleName() + ").");
            }
        }
        if (Files.exists(arquivoEnv)) {
            try {
                String conteudo = Files.readString(arquivoEnv, StandardCharsets.UTF_8);
                if (conteudo.startsWith("\uFEFF")) {
                    conteudo = conteudo.substring(1);
                }
                try (StringReader reader = new StringReader(conteudo)) {
                    env.load(reader);
                }
                ok("Achei o arquivo " + arquivoEnv + "; valores não serão exibidos.");
            } catch (IOException e) {
                erro("Não consegui ler " + arquivoEnv + " (" + e.getClass().getSimpleName() + ").");
            }
        } else {
            aviso("Não achei o arquivo " + arquivoEnv + " na pasta atual. Vou usar só as variáveis de ambiente do sistema. "
                    + "Se você usa .env, rode a partir da pasta raiz do projeto (a que tem o pom.xml).");
        }
    }

    /** Resolve propriedades de sistema/ambiente antes dos arquivos de configuração. */
    private String valor(Properties padroes, Properties env, String chave) {
        String direto = System.getProperty(chave);
        if (direto == null) {
            direto = System.getenv(nomeVariavelAmbiente(chave));
        }
        if (direto != null) {
            return direto.trim();
        }
        String bruto = padroes.getProperty(chave);
        if (bruto == null) {
            return env.getProperty(chave, "").trim();
        }
        Matcher m = MARCADOR.matcher(bruto);
        StringBuilder resultado = new StringBuilder();
        while (m.find()) {
            String nome = m.group(1);
            String substituto = System.getProperty(nome);
            if (substituto == null) {
                substituto = System.getenv(nome);
            }
            if (substituto == null) {
                substituto = env.getProperty(nome);
            }
            if (substituto == null) {
                substituto = m.group(2) != null ? m.group(2) : "";
            }
            m.appendReplacement(resultado, Matcher.quoteReplacement(substituto));
        }
        m.appendTail(resultado);
        return resultado.toString().trim();
    }

    private static String nomeVariavelAmbiente(String propriedade) {
        return propriedade.replace('.', '_').replace('-', '_').toUpperCase(Locale.ROOT);
    }

    // ---------------------------------------------------------------- conferência dos valores

    private void conferirDbUrl(String dbUrl, String url) {
        if (!dbUrl.isBlank()) {
            ok("DB_URL está carregada (valor omitido).");
        } else if (!url.isBlank()) {
            aviso("DB_URL não está definida; será usada a propriedade spring.datasource.url direta (valor omitido).");
        } else {
            erro("DB_URL não foi carregada. Confira o .env no diretório de trabalho ou a variável de ambiente DB_URL.");
        }
    }

    private void conferirPerfil(Properties padroes, Properties env) {
        String perfil = valor(padroes, env, "spring.profiles.active");
        if (List.of(perfil.split(",")).stream().map(String::trim).anyMatch("banco"::equals)) {
            ok("Perfil Spring ativo: banco.");
        } else {
            aviso("Perfil Spring banco não está ativo (perfil detectado: "
                    + (perfil.isBlank() ? "default/H2" : perfil) + ").");
        }
    }

    private URI conferirUrl(String url) {
        if (url.isBlank()) {
            erro("DB_URL está vazia ou não existe (spring.datasource.url).");
            return null;
        }
        if (url.startsWith("\"") || url.endsWith("\"")) {
            erro("A URL está entre aspas. Tire as aspas.");
        }
        if (url.startsWith("postgresql://") || url.startsWith("postgres://")) {
            erro("A URL é a string \"URI\" do Supabase. No Java ela precisa começar com jdbc:postgresql://");
        }
        if (url.contains("@")) {
            erro("A URL contém informação de usuário. Mantenha usuário e senha somente em DB_USER e DB_PASSWORD.");
        }
        if (url.contains("[") || url.contains("]")) {
            erro("A URL tem colchetes [ ]. Eles são só um aviso do Supabase (ex.: [YOUR-PASSWORD]); não devem existir.");
        }
        if (!url.startsWith("jdbc:postgresql://")) {
            erro("A URL JDBC deve começar com jdbc:postgresql://; o valor não será exibido.");
            return null;
        }
        try {
            URI uri = URI.create(url.substring("jdbc:".length()));
            if (uri.getHost() == null) {
                erro("Não consegui achar o endereço do servidor na URL.");
                return null;
            }
            ok("URL no formato certo. Servidor: " + uri.getHost() + ", porta " + (uri.getPort() == -1 ? 5432 : uri.getPort()) + ".");
            return uri;
        } catch (IllegalArgumentException e) {
            erro("A URL tem caracteres inválidos (valor não exibido).");
            return null;
        }
    }

    private void conferirUsuario(String usuario, URI uri) {
        if (usuario.isBlank()) {
            erro("DB_USER está vazio ou não existe (spring.datasource.username).");
            return;
        }
        String host = uri == null ? "" : uri.getHost();
        if (host.contains("pooler.supabase.com") && !usuario.contains(".")) {
            erro("No pooler do Supabase o usuário deve incluir o identificador do projeto (postgres.<código-do-projeto>).");
        } else if (usuario.contains("<") || usuario.contains(">") || usuario.endsWith("CODIGO_DO_SEU_PROJETO")) {
            erro("DB_USER ainda contém um marcador de exemplo. Substitua-o pelos dados do projeto.");
        } else if (host.startsWith("db.") && host.endsWith(".supabase.co") && usuario.contains(".")) {
            aviso("Na conexão direta (db.xxx.supabase.co) o usuário é só \"postgres\".");
        } else {
            ok("DB_USER está configurada (valor não exibido).");
        }
    }

    private void conferirSenha(String senha) {
        if (senha.isBlank()) {
            erro("DB_PASSWORD está vazia ou não existe.");
            return;
        }
        if (senha.startsWith("[") || senha.startsWith("<") || senha.contains("YOUR-PASSWORD")) {
            erro("A senha ainda é um texto de exemplo. Coloque a senha real do banco.");
            return;
        }
        ok("Senha preenchida (" + senha.length() + " caracteres, não exibida).");
        if (!senha.matches("[A-Za-z0-9]+")) {
            aviso("A senha tem espaços ou símbolos. Se a conexão der \"password authentication failed\", redefina a senha "
                    + "no Supabase (Project Settings > Database) para uma só com letras e números.");
        }
    }

    private void conferirJwt(String jwt) {
        if (jwt.length() < 32) {
            erro("JWT_SEGREDO precisa ter pelo menos 32 caracteres (agora tem " + jwt.length()
                    + "). Sem isso o sistema NÃO inicia.");
        } else {
            ok("Segredo do JWT com " + jwt.length() + " caracteres.");
        }
    }

    private void conferirAdmin(String adminSenha) {
        if (adminSenha.isBlank()) {
            aviso("ADMIN_SENHA está vazia: nenhum administrador será criado (você não conseguiria resolver ocorrências).");
        } else {
            ok("Senha do administrador definida.");
        }
    }

    // ---------------------------------------------------------------- rede e banco

    private boolean conferirRede(String host, int porta) {
        InetAddress[] enderecos;
        try {
            enderecos = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            erro("Não consegui descobrir o endereço de " + host + ". Sem internet/DNS, ou o nome do servidor está errado.");
            return false;
        }
        boolean soIpv6 = true;
        StringBuilder lista = new StringBuilder();
        for (InetAddress a : enderecos) {
            lista.append(a.getHostAddress()).append(' ');
            if (!(a instanceof Inet6Address)) {
                soIpv6 = false;
            }
        }
        ok("Endereço encontrado: " + lista.toString().trim());
        if (soIpv6) {
            aviso("O servidor só tem endereço IPv6 (típico da conexão DIRETA do Supabase). Se sua rede não tem IPv6, "
                    + "use o Session pooler (host aws-...pooler.supabase.com).");
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, porta), 6000);
            ok("Consegui abrir a conexão de rede com " + host + ":" + porta + ".");
            return true;
        } catch (IOException e) {
            erro("Não consegui conectar em " + host + ":" + porta + " (" + e.getClass().getSimpleName() + "). "
                    + "Causas comuns: sem internet, rede/firewall bloqueando a porta, projeto do Supabase PAUSADO "
                    + "(abra o painel e clique em Restore), ou conexão direta sem IPv6.");
            return false;
        }
    }

    private void conferirBanco(String url, String usuario, String senha) {
        try (Connection conexao = DriverManager.getConnection(url, usuario, senha);
             Statement comando = conexao.createStatement();
             ResultSet rs = comando.executeQuery("select 1")) {
            rs.next();
            ok("Conexão PostgreSQL estabelecida com sucesso.");

            DatabaseMetaData meta = conexao.getMetaData();
            List<String> tabelas = new ArrayList<>();
            try (ResultSet t = meta.getTables(null, "public", "%", new String[]{"TABLE"})) {
                while (t.next()) {
                    tabelas.add(t.getString("TABLE_NAME"));
                }
            }
            saida.println("      Tabelas em public: " + (tabelas.isEmpty() ? "(nenhuma ainda: o sistema cria ao iniciar)" : tabelas));
        } catch (SQLException e) {
            erro("O banco recusou ou a conexão falhou: " + explicar(e));
        }
    }

    private static String explicar(SQLException e) {
        StringBuilder mensagens = new StringBuilder();
        for (SQLException atual = e; atual != null; atual = atual.getNextException()) {
            mensagens.append(' ').append(String.valueOf(atual.getMessage()));
        }
        String m = mensagens.toString().toLowerCase(Locale.ROOT);
        if (m.contains("no suitable driver")) {
            return "o driver do PostgreSQL não está no projeto. Confira a dependência org.postgresql:postgresql no pom.xml e recarregue o Maven.";
        }
        if (m.contains("tenant or user not found")) {
            return "\"Tenant or user not found\": o usuário deve ser postgres.<código-do-projeto> e o host deve ser o do pooler da SUA região "
                    + "(copie de Connect > Session pooler no painel do Supabase).";
        }
        if (m.contains("password authentication failed")) {
            return "senha incorreta. Redefina a senha em Project Settings > Database e atualize o .env.";
        }
        if (m.contains("ssl")) {
            return "problema de SSL: mantenha ?sslmode=require no fim da URL.";
        }
        if (m.contains("timed out") || m.contains("timeout")) {
            return "tempo esgotado: rede bloqueada ou projeto pausado.";
        }
        if (m.contains("unknownhost") || m.contains("could not translate host") || m.contains("name or service not known")) {
            return "falha de DNS ao localizar o servidor. Confira o host configurado e a rede.";
        }
        if (m.contains("connection refused") || m.contains("connection attempt failed")) {
            return "conexão recusada pelo host/porta. Confira o endpoint do Supabase, a porta e o estado do projeto.";
        }
        String sqlState = e.getSQLState();
        return "falha JDBC (SQLState " + (sqlState == null ? "indisponível" : sqlState)
                + "). Confira endpoint, porta, SSL e credenciais; detalhes brutos foram omitidos.";
    }

    // ---------------------------------------------------------------- saída

    private void ok(String texto) {
        saida.println("[OK]    " + texto);
    }

    private void aviso(String texto) {
        saida.println("[AVISO] " + texto);
    }

    private void erro(String texto) {
        erros++;
        saida.println("[ERRO]  " + texto);
    }
}
