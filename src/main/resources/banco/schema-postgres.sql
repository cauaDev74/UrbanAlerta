-- =====================================================================
-- UrbanAlerta - script de criação das tabelas (PostgreSQL / Supabase)
-- OPCIONAL: com spring.jpa.hibernate.ddl-auto=update o Hibernate já cria as tabelas sozinho.
-- Use este script se preferir criar à mão (e então ddl-auto=validate).
-- Reflete as classes Usuario e Ocorrencia. Os enums do Java são guardados como texto.
-- =====================================================================
CREATE TABLE IF NOT EXISTS usuario (
    id          UUID PRIMARY KEY,
    nome        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    senha_hash  VARCHAR(200) NOT NULL,                       -- hash PBKDF2, nunca a senha
    perfil      VARCHAR(20)  NOT NULL CHECK (perfil IN ('CLIENTE', 'ADMIN')),
    criado_em   TIMESTAMP    NOT NULL,
    versao      BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS ocorrencia (
    id                  UUID PRIMARY KEY,
    tipo                VARCHAR(40)  NOT NULL
        CHECK (tipo IN ('BURACO', 'SEM_ILUMINACAO', 'ALTO_INDICE_ACIDENTES',
                        'LIXO_ACUMULADO', 'ESGOTO_A_CEU_ABERTO', 'SEMAFORO_QUEBRADO')),
    endereco            VARCHAR(300) NOT NULL,
    latitude            DOUBLE PRECISION NOT NULL,
    longitude           DOUBLE PRECISION NOT NULL,
    data_hora_registro  TIMESTAMP    NOT NULL,
    descricao           VARCHAR(1000),
    status              VARCHAR(20)  NOT NULL
        CHECK (status IN ('PENDENTE', 'EM_ANALISE', 'RESOLVIDO')),
    atualizado_em       TIMESTAMP    NOT NULL,
    autor_id            UUID REFERENCES usuario (id),         -- cliente que registrou
    responsavel_id      UUID REFERENCES usuario (id),         -- administrador que mudou o status
    versao              BIGINT       NOT NULL DEFAULT 0       -- controle de versão (lock otimista)
);

CREATE INDEX IF NOT EXISTS idx_ocorrencia_status ON ocorrencia (status);
CREATE INDEX IF NOT EXISTS idx_ocorrencia_data   ON ocorrencia (data_hora_registro);

-- SUPABASE: as tabelas do schema public ficam expostas pela API pública do Supabase.
-- Ative o RLS (sem políticas) para bloquear essa API; o usuário postgres usado pelo Java ignora o RLS.
ALTER TABLE usuario    ENABLE ROW LEVEL SECURITY;
ALTER TABLE ocorrencia ENABLE ROW LEVEL SECURITY;
