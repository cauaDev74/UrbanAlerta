# UrbanAlerta — Protótipo (cliente e administrador)

Sistema web para identificação e denúncia de problemas de infraestrutura urbana.
Projeto da disciplina de Sistemas Operacionais, IFBA Campus Irecê.

- **Cliente:** cria conta, registra ocorrências, vê o mapa e acompanha as suas.
- **Administrador:** acompanha um dashboard com gráficos e indicadores, gerencia a fila e **resolve** ocorrências (Pendente, Em análise, Resolvido).
- **Back-end 100% Java**, em camadas (Spring Boot + JPA/Hibernate), com login por JWT.
- **Banco:** PostgreSQL (por exemplo, Supabase), configurado por variáveis de ambiente ou pelo arquivo `.env`.

## Como rodar

Pré-requisitos: Java 17 ou superior e Maven 3.9 ou superior (ou uma IDE que abra projetos Maven).

```bash
mvn spring-boot:run
```

Configure o PostgreSQL, `JWT_SEGREDO` e as credenciais do administrador no `.env` conforme a seção [Conectar o banco](#conectar-o-seu-banco-perfil-banco-com-o-env), depois abra **http://localhost:8080**. Não existem contas ou ocorrências de demonstração: clientes devem criar uma conta pela tela e o administrador é provisionado com `ADMIN_EMAIL` e `ADMIN_SENHA`.

O mapa e a conversão de endereços precisam de internet (OpenStreetMap e Nominatim). Testes: `mvn test`.

### Acessar pelo celular

A interface se adapta a celulares e pode ser aberta no navegador, sem instalar um aplicativo:

1. Inicie o sistema no computador com `mvn spring-boot:run`.
2. Conecte o celular e o computador à mesma rede Wi-Fi.
3. No computador, consulte o endereço IPv4 da rede local (por exemplo, com `ipconfig` no Windows).
4. No navegador do celular, abra `http://IP-DO-COMPUTADOR:8080` (substitua pelo endereço encontrado; não use `localhost`).

Se a página não abrir, permita conexões de entrada na porta 8080 no firewall do computador e confira se a rede permite comunicação entre dispositivos. O mapa e a busca de endereço também precisam de internet.

## Quem pode fazer o quê

| Ação | Cliente | Administrador |
|---|---|---|
| Criar conta e entrar | sim | entra (a conta é criada pelo sistema) |
| Ver o mapa e todas as ocorrências | sim | sim |
| Ver o nome de quem registrou | só nas próprias | em todas |
| Registrar ocorrência | **sim** | não |
| Mudar o status | não | **sim** |
| Ver indicadores e fila de gestão | não | sim |

A regra é aplicada no servidor (interceptor + serviço); esconder botões na tela é só conforto.

## Arquitetura em camadas

```
Navegador (HTML/JS)  --HTTP/JSON-->  controller  -->  service  -->  repository  -->  banco (JPA/Hibernate)
                                         |              |
                                        dto           model (entidades e regras)
        security (JWT, senha, interceptor) e exception (erros) atravessam as camadas
```

```
src/main/java/com/urbanalerta/
├── controller/   recebe HTTP e chama o serviço (AuthController, OcorrenciaController, SistemaController)
├── service/      casos de uso e regras de acesso (AuthService, OcorrenciaService, geocodificação Nominatim)
├── repository/   acesso a dados com Spring Data JPA (OcorrenciaRepository, UsuarioRepository)
├── model/        entidades JPA e regras do negócio (Usuario, Ocorrencia, StatusOcorrencia, ...)
├── dto/          o que entra e sai da API (nunca expõe senha nem hash)
├── security/     SenhaService (PBKDF2), JwtService (HS256), AutenticacaoInterceptor, @ExigePerfil
├── exception/    exceções do sistema e TratadorDeErros (401, 403, 404, 409, 422...)
└── config/       ConfiguracaoWeb (rotas protegidas) e InicializadorDeDados (admin configurado)
src/main/resources/
├── static/       interface (HTML, CSS, JS puro + Leaflet.js)
├── banco/        schema-postgres.sql (alternativa ao ddl-auto)
├── application.properties         (PostgreSQL)
└── application-banco.properties   (configurações do perfil banco)
```

## Segurança

- **Senhas** guardadas com PBKDF2-HMAC-SHA256, sal aleatório e 210 mil iterações (nunca a senha em si).
- **Login** devolve um **token JWT** (HS256, validade de 8 horas) que a tela envia em `Authorization: Bearer ...`.
- **Interceptor** confere o token, carrega o usuário do banco (o perfil vem do banco, não do token) e aplica `@ExigePerfil`.
- Login com senha errada e com e-mail inexistente dão a mesma resposta, e gastam o mesmo tempo.
- É uma implementação pequena e didática. Em produção, prefira o Spring Security.

## API REST

| Método | Caminho | Quem | O que faz |
|---|---|---|---|
| POST | `/auth/cadastro` | público | Cria conta de cliente e devolve o token |
| POST | `/auth/login` | público | Devolve o token JWT |
| GET | `/auth/eu` | logado | Dados do usuário do token |
| GET | `/sistema` | público | Onde os dados ficam |
| GET | `/tipos-ocorrencia` | logado | Tipos de ocorrência |
| GET | `/ocorrencias` | logado | Lista as ocorrências |
| GET | `/ocorrencias/{id}` | logado | Detalhe |
| POST | `/ocorrencias` | **cliente** | Registra (o servidor geocodifica o endereço) |
| PATCH | `/ocorrencias/{id}/status` | **admin** | Muda o status, na ordem do ciclo |

Erros vêm como `{"erro": "mensagem em português"}`: **401** sem login ou token vencido, **403** perfil sem permissão, **409** pular etapa do status, e-mail repetido ou alteração simultânea, **422** endereço não encontrado, **502** geocodificação fora do ar, **400** dados inválidos, **404** não encontrada.

## Banco de dados (JPA + Hibernate)

Tabelas `usuario` e `ocorrencia`, criadas ou atualizadas pelo Hibernate (`update` por padrão). `ocorrencia` tem `autor_id` (cliente) e `responsavel_id` (administrador que mudou o status). Há lock otimista (`@Version`) e índices em `status` e `data_hora_registro`. O `schema-postgres.sql` é uma alternativa para criar tudo à mão.

### Conectar o seu banco (perfil `banco`) com o `.env`

Toda a configuração secreta fica em **um único arquivo, o `.env`**, que nunca vai para o GitHub.

1. Copie o modelo [`.env.example`](./.env.example) para `.env` e preencha (no Windows: copie o arquivo e renomeie a cópia para `.env`).
2. Preencha `DB_URL` e `DB_USER` com os dados do Supabase (Connect, **Session pooler**: o usuário é `postgres.<código-do-projeto>`), `DB_PASSWORD` com a senha do banco, `JWT_SEGREDO` com um texto aleatório de 32+ caracteres (`openssl rand -base64 48`) e `ADMIN_EMAIL`/`ADMIN_SENHA` com as credenciais do administrador.
3. No IntelliJ IDEA, abra **Run > Edit Configurations...**, selecione `UrbanAlertaApplication` e defina **Working directory** como `$PROJECT_DIR$`. Selecione o perfil `banco` em **Active profiles** (ou use `-Dspring.profiles.active=banco` nas VM options). O diretório é importante: o Spring carrega `.env` de forma relativa à pasta de execução da JVM.
4. Rode o sistema; no topo da tela deve aparecer "Dados: Banco de dados (PostgreSQL)". O projeto não inclui H2; um PostgreSQL configurado é obrigatório.
5. **Se algo não conectar, rode a ferramenta de diagnóstico:** no IntelliJ, botão direito em `src/main/java/com/urbanalerta/tools/TesteConexao.java`, Run. Configure também o diretório de trabalho como `$PROJECT_DIR$`. Ela verifica a configuração e tenta conectar, sem imprimir a senha.
6. **Supabase:** ative o RLS nas duas tabelas (comandos no fim do `schema-postgres.sql`). A tabela `usuario` guarda hashes de senha e, sem isso, a API pública do Supabase pode expô-los.

O Spring também procura `config/application-banco.properties` no diretório de trabalho. Se esse arquivo local existir, as propriedades nele definidas podem sobrescrever as do perfil empacotado (inclusive senha, JWT e admin). A pasta `config/` é ignorada pelo Git; evite manter valores conflitantes entre ela e `.env`.

Regras do `.env`: sem aspas, sem comentário no fim da linha e, de preferência, sem espaços, acentos nem os símbolos `$ { } # \` nas senhas. O administrador é criado na primeira execução; trocar `ADMIN_SENHA` depois não altera uma conta que já existe.

## Relação com Sistemas Operacionais

- **Processos e threads:** o Tomcat embutido atende cada requisição em uma thread; o pool de conexões é compartilhado entre elas.
- **Concorrência:** `atualizarStatus` é `synchronized` e a entidade tem `@Version`: duas pessoas mudando a mesma ocorrência não se sobrescrevem.
- **Redes:** o Nominatim e o banco são acessados por sockets TCP/IP (e DNS).
- **Arquivos e armazenamento:** o PostgreSQL grava os dados em disco.
- **Segurança e permissões:** perfis (cliente/admin) e autenticação controlam quem acessa cada recurso.

## Publicar: GitHub e Vercel

A Vercel roda o projeto **inteiro** (API Java + interface) a partir do `Dockerfile.vercel` (Container Runtime, em beta). Como o Spring já serve a interface, tudo fica no mesmo endereço, sem CORS.

1. **GitHub:** suba o repositório (o `.env` e a pasta `config/` não podem aparecer no `git status`).
2. **Vercel:** em vercel.com/new, importe o repositório. A Vercel detecta o `Dockerfile.vercel` sozinha.
3. **Variáveis de ambiente** (na tela de importação ou em Settings, Environment Variables):
   `SPRING_PROFILES_ACTIVE=banco`, `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SEGREDO`, `ADMIN_EMAIL` e `ADMIN_SENHA`. Use os mesmos valores do `.env`. Não defina `PORT`: a Vercel define.
4. **Deploy** e teste: entre com o administrador provisionado por `ADMIN_EMAIL` e `ADMIN_SENHA`; crie uma conta de cliente pela interface para registrar ocorrências.

Pontos de atenção:

- **Beta e partida a frio:** a função escala a zero quando fica sem uso, e a primeira requisição depois disso espera o Spring Boot iniciar (alguns segundos). Se isso for um problema na apresentação, abra o site antes.
- **Sem disco persistente:** os dados ficam só no Supabase.
- **Conexões:** o pool é pequeno por instância (`DB_POOL_MAX`, padrão 3) para não esgotar o limite do Supabase quando a Vercel criar várias instâncias.
- **Separar interface e API (opcional):** preencha `API_PRODUCAO` em `static/js/config.js` e cadastre `URBANALERTA_CORS_ORIGENS` (site da interface) na API.
- **Outras hospedagens** (Render, Railway, Fly.io): use o mesmo conteúdo do `Dockerfile.vercel` em um arquivo chamado `Dockerfile`.

## Limitações

- Sem recuperação de senha, confirmação de e-mail ou limite de tentativas de login.
- Sem foto como evidência, confirmação comunitária ou notificações.
