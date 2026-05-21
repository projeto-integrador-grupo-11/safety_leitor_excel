-- =====================================================================
-- Schema das tabelas alimentadas pelo JAR safety_leitor_excel.
-- Execute este script no banco antes de rodar o JAR pela primeira vez.
-- Conexão padrão: config.properties (jdbc:mysql://.../safety_intelligence)
-- =====================================================================

-- Tabela legada usada pelo MunicipioRepository (IDHM)
CREATE TABLE IF NOT EXISTS municipio (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    nome        VARCHAR(120) NOT NULL,
    idhm_geral  DOUBLE,
    renda       DOUBLE,
    educacao    DOUBLE,
    longevidade DOUBLE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

-- População municipal (planilha populacao_municipios_2025.xls)
CREATE TABLE IF NOT EXISTS populacao_municipio (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    uf             CHAR(2)      NOT NULL,
    nome_municipio VARCHAR(120) NOT NULL,
    populacao      INT          NOT NULL,
    ano            INT          NOT NULL,
    INDEX idx_pop_mun_uf       (uf),
    INDEX idx_pop_mun_uf_nome  (uf, nome_municipio)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

-- Indicadores agregados por UF/ano (planilha indicadores_seguranca_publica.xlsx)
CREATE TABLE IF NOT EXISTS indicador_seguranca (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    uf          CHAR(2)     NOT NULL,
    tipo        VARCHAR(80) NOT NULL,
    ano         INT         NOT NULL,
    quantidade  INT         NOT NULL,
    INDEX idx_ind_uf_tipo_ano (uf, tipo, ano)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

-- Ocorrências mensais de latrocínio e homicídio doloso
-- (planilhas banco_seguranca_2021..2025.xlsx)
CREATE TABLE IF NOT EXISTS ocorrencia_seguranca (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    uf             CHAR(2)      NOT NULL,
    nome_municipio VARCHAR(120) NOT NULL,
    evento         VARCHAR(80)  NOT NULL,
    ano_ref        INT          NOT NULL,
    mes_ref        INT          NOT NULL,
    qtd_vitimas    INT          NOT NULL,
    INDEX idx_ocorr_uf_mun     (uf, nome_municipio),
    INDEX idx_ocorr_uf_evento  (uf, evento, ano_ref),
    INDEX idx_ocorr_ano        (ano_ref)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

-- Tabela usada pelo LogRepository (já deve existir no banco)
CREATE TABLE IF NOT EXISTS log_sistema (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    nivel     VARCHAR(20)   NOT NULL,
    mensagem  VARCHAR(500)  NOT NULL,
    data_hora DATETIME      NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;
