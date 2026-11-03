# UrbanAlerta — Protótipo (cliente e administrador)

Sistema web para identificação e denúncia de problemas de infraestrutura urbana.
Projeto da disciplina de Sistemas Operacionais, IFBA Campus Irecê.

- **Cliente:** cria conta, registra ocorrências, vê o mapa e acompanha as suas.
- **Administrador:** vê todas as ocorrências, os indicadores e **resolve** (Pendente, Em análise, Resolvido).
- **Back-end 100% Java**, em camadas (Spring Boot + JPA/Hibernate), com login por JWT.
- **Banco:** H2 por padrão (não precisa instalar nada) ou o seu banco (PostgreSQL/Supabase) com o perfil `banco`.

## Como rodar

Pré-requisitos: Java 17 ou superior e Maven 3.9 ou superior (ou uma IDE que abra projetos Maven).

```bash
mvn spring-boot:run
```

Abra **http://localhost:8080**. Na tela de acesso aparecem botões das **contas de demonstração**:

| Conta | E-mail | Senha |
|---|---|---|
| Cliente | `cliente@urbanalerta.local` | `cliente123` |
| Cliente (outro) | `vizinho@urbanalerta.local` | `cliente123` |
| Administrador | `admin@urbanalerta.local` | `admin123` |

Essas contas e 6 ocorrências **fictícias** (endereços inventados, coordenadas aproximadas de Irecê) só existem no modo padrão. No modo `banco` não são criadas.

O mapa e a conversão de endereços precisam de internet (OpenStreetMap e Nominatim). Testes: `mvn test`.

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
└── config/       ConfiguracaoWeb (rotas protegidas) e InicializadorDeDados (admin e demonstração)
src/main/resources/
├── static/       interface (HTML, CSS, JS puro + Leaflet.js)
├── banco/        schema-postgres.sql (alternativa ao ddl-auto)
├── application.properties         (padrão: H2)
└── application-banco.properties   (perfil banco: Supabase/PostgreSQL)
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
| GET | `/sistema` | público | Onde os dados ficam e se há contas de demonstração |
| GET | `/tipos-ocorrencia` | logado | Tipos de ocorrência |
| GET | `/ocorrencias` | logado | Lista as ocorrências |
| GET | `/ocorrencias/{id}` | logado | Detalhe |
| POST | `/ocorrencias` | **cliente** | Registra (o servidor geocodifica o endereço) |
| PATCH | `/ocorrencias/{id}/status` | **admin** | Muda o status, na ordem do ciclo |

Erros vêm como `{"erro": "mensagem em português"}`: **401** sem login ou token vencido, **403** perfil sem permissão, **409** pular etapa do status, e-mail repetido ou alteração simultânea, **422** endereço não encontrado, **502** geocodificação fora do ar, **400** dados inválidos, **404** não encontrada.

## Banco de dados (JPA + Hibernate)

Tabelas `usuario` e `ocorrencia`, criadas pelo Hibernate (`create-drop` no H2, `update` no seu banco). `ocorrencia` tem `autor_id` (cliente) e `responsavel_id` (administrador que mudou o status). Há lock otimista (`@Version`) e índices em `status` e `data_hora_registro`. O `schema-postgres.sql` é uma alternativa para criar tudo à mão.

### Conectar o seu banco (perfil `banco`) com o `.env`

Toda a configuração secreta fica em **um único arquivo, o `.env`**, que nunca vai para o GitHub.

1. Copie o modelo [`.env.example`](./.env.example) para `.env` e preencha (no Windows: copie o arquivo e renomeie a cópia para `.env`).
2. Preencha `DB_URL` e `DB_USER` com os dados do Supabase (Connect, **Session pooler**: o usuário é `postgres.<código-do-projeto>`), `DB_PASSWORD` com a senha do banco, `JWT_SEGREDO` com um texto aleatório de 32+ caracteres (`openssl rand -base64 48`) e `ADMIN_SENHA` com a senha do administrador.
3. No IntelliJ IDEA, abra **Run > Edit Configurations...**, selecione `UrbanAlertaApplication` e defina **Working directory** como `$PROJECT_DIR$`. Selecione o perfil `banco` em **Active profiles** (ou use `-Dspring.profiles.active=banco` nas VM options). O diretório é importante: o Spring carrega `.env` de forma relativa à pasta de execução da JVM.
4. Rode o sistema; no topo da tela deve aparecer "Dados: Banco de dados (PostgreSQL)". Para usar o H2 de demonstração, remova `spring.profiles.active=banco` do `.env` e não selecione o perfil `banco` na configuração de execução.
5. **Se algo não conectar, rode a ferramenta de diagnóstico:** no IntelliJ, botão direito em `src/main/java/com/urbanalerta/tools/TesteConexao.java`, Run. Configure também o diretório de trabalho como `$PROJECT_DIR$`. Ela verifica a configuração e tenta conectar, sem imprimir a senha.
6. **Supabase:** ative o RLS nas duas tabelas (comandos no fim do `schema-postgres.sql`). A tabela `usuario` guarda hashes de senha e, sem isso, a API pública do Supabase pode expô-los.

O Spring também procura `config/application-banco.properties` no diretório de trabalho. Se esse arquivo local existir, as propriedades nele definidas podem sobrescrever as do perfil empacotado (inclusive senha, JWT e admin). A pasta `config/` é ignorada pelo Git; evite manter valores conflitantes entre ela e `.env`.

Regras do `.env`: sem aspas, sem comentário no fim da linha e, de preferência, sem espaços, acentos nem os símbolos `$ { } # \` nas senhas. O administrador é criado na primeira execução; trocar `ADMIN_SENHA` depois não altera uma conta que já existe.

## Relação com Sistemas Operacionais

- **Processos e threads:** o Tomcat embutido atende cada requisição em uma thread; o pool de conexões é compartilhado entre elas.
- **Concorrência:** `atualizarStatus` é `synchronized` e a entidade tem `@Version`: duas pessoas mudando a mesma ocorrência não se sobrescrevem.
- **Redes:** o Nominatim e o banco são acessados por sockets TCP/IP (e DNS).
- **Memória:** o H2 em memória vive no heap da JVM e some ao reiniciar.
- **Arquivos e armazenamento:** com o perfil `banco`, o PostgreSQL grava os dados em disco.
- **Segurança e permissões:** perfis (cliente/admin) e autenticação controlam quem acessa cada recurso.

## Publicar: GitHub e Vercel

A Vercel roda o projeto **inteiro** (API Java + interface) a partir do `Dockerfile.vercel` (Container Runtime, em beta). Como o Spring já serve a interface, tudo fica no mesmo endereço, sem CORS.

1. **GitHub:** suba o repositório (o `.env` e a pasta `config/` não podem aparecer no `git status`).
2. **Vercel:** em vercel.com/new, importe o repositório. A Vercel detecta o `Dockerfile.vercel` sozinha.
3. **Variáveis de ambiente** (na tela de importação ou em Settings, Environment Variables):
   `SPRING_PROFILES_ACTIVE=banco`, `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SEGREDO`, `ADMIN_SENHA` e, se quiser, `ADMIN_EMAIL`. Use os mesmos valores do `.env`. Não defina `PORT`: a Vercel define.
4. **Deploy** e teste: entre como administrador, registre uma ocorrência como cliente e resolva como administrador.

Pontos de atenção:

- **Beta e partida a frio:** a função escala a zero quando fica sem uso, e a primeira requisição depois disso espera o Spring Boot iniciar (alguns segundos). Se isso for um problema na apresentação, abra o site antes.
- **Sem disco persistente:** os dados ficam só no Supabase.
- **Conexões:** o pool é pequeno por instância (`DB_POOL_MAX`, padrão 3) para não esgotar o limite do Supabase quando a Vercel criar várias instâncias.
- **Separar interface e API (opcional):** preencha `API_PRODUCAO` em `static/js/config.js` e cadastre `URBANALERTA_CORS_ORIGENS` (site da interface) na API.
- **Outras hospedagens** (Render, Railway, Fly.io): use o mesmo conteúdo do `Dockerfile.vercel` em um arquivo chamado `Dockerfile`.

## Limitações

- Sem recuperação de senha, confirmação de e-mail ou limite de tentativas de login.
- Sem foto como evidência, confirmação comunitária ou notificações.
- O console do H2 (`/h2-console`) é só para demonstração local; desligue-o (`spring.h2.console.enabled=false`) fora disso.
