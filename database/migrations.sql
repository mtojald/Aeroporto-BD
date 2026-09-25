-- =====================================================================
-- 1. AEROPORTO
-- =====================================================================

create database aeroporto
use aeroporto

CREATE TABLE Aeroporto (
    cod_iata    VARCHAR(3)   NOT NULL,
    nome        VARCHAR(100) NOT NULL,
    cidade      VARCHAR(50),
    pais        VARCHAR(50),
    CONSTRAINT pk_aeroporto PRIMARY KEY (cod_iata)
);

-- =====================================================================
-- 2. TERMINAL
-- =====================================================================
CREATE TABLE Terminal (
    num_terminal INT NOT NULL,
    piso         INT,
    CONSTRAINT pk_terminal PRIMARY KEY (num_terminal)
);

-- =====================================================================
-- 3. PORTAO [entidade fraca de Terminal]
-- =====================================================================
CREATE TABLE Portao (
    num_portao   INT NOT NULL,
    num_terminal INT NOT NULL,
    CONSTRAINT pk_portao PRIMARY KEY (num_portao, num_terminal),
    CONSTRAINT fk_portao_terminal FOREIGN KEY (num_terminal)
        REFERENCES Terminal (num_terminal)
        ON DELETE CASCADE
);

-- =====================================================================
-- 4. COMPANHIA AEREA
-- =====================================================================
CREATE TABLE CompanhiaAerea (
    cod_cia VARCHAR(10)  NOT NULL,
    nome    VARCHAR(100) NOT NULL,
    CONSTRAINT pk_companhia_aerea PRIMARY KEY (cod_cia)
);

-- Atributo multivalorado: telefone(s) da companhia aérea
CREATE TABLE CompanhiaAerea_Telefone (
    cod_cia  VARCHAR(10) NOT NULL,
    telefone VARCHAR(20) NOT NULL,
    CONSTRAINT pk_companhia_telefone PRIMARY KEY (cod_cia, telefone),
    CONSTRAINT fk_companhia_telefone_cia FOREIGN KEY (cod_cia)
        REFERENCES CompanhiaAerea (cod_cia)
        ON DELETE CASCADE
);

-- =====================================================================
-- 5. AERONAVE
-- =====================================================================
CREATE TABLE Aeronave (
    cod_aeronave              VARCHAR(10) NOT NULL,
    modelo                    VARCHAR(50),
    capac_passageiros         INT check (capac_passageiros < 300),
    ano_fabricacao            INT,
    fk_CompanhiaAerea_cod_cia VARCHAR(10) NOT NULL,
    CONSTRAINT pk_aeronave PRIMARY KEY (cod_aeronave),
    CONSTRAINT fk_aeronave_companhia FOREIGN KEY (fk_CompanhiaAerea_cod_cia)
        REFERENCES CompanhiaAerea (cod_cia)
);

-- =====================================================================
-- 6. FUNCIONARIO (superclasse: Piloto, Comissario, Mecanico)
-- =====================================================================
CREATE TABLE Funcionario (
    matricula        INT          NOT NULL,
    primeiro_nome    VARCHAR(50)  NOT NULL,
    sobrenome        VARCHAR(50)  NOT NULL,
    cpf              CHAR(11)     NOT NULL,
    data_nasci       DATE,
    email            VARCHAR(100),
    endereco_cep     CHAR(9),
    endereco_rua     VARCHAR(100),
    endereco_numero  VARCHAR(10),
    endereco_bairro  VARCHAR(50),
    CONSTRAINT pk_funcionario PRIMARY KEY (matricula),
    CONSTRAINT uq_funcionario_cpf UNIQUE (cpf)
);

-- Atributo multivalorado: telefone(s) do funcionário
CREATE TABLE Funcionario_Telefone (
    matricula INT         NOT NULL,
    telefone  VARCHAR(20) NOT NULL,
    CONSTRAINT pk_funcionario_telefone PRIMARY KEY (matricula, telefone),
    CONSTRAINT fk_funcionario_telefone_func FOREIGN KEY (matricula)
        REFERENCES Funcionario (matricula)
        ON DELETE CASCADE
);

-- =====================================================================
-- 7. PILOTO [especialização de Funcionario]
-- =====================================================================
CREATE TABLE Piloto (
    fk_Funcionario_matricula INT          NOT NULL,
    num_licenca              VARCHAR(20)  NOT NULL,
    horas_voo_acumuladas     DECIMAL(8,2) DEFAULT 0,
    CONSTRAINT pk_piloto PRIMARY KEY (fk_Funcionario_matricula),
    CONSTRAINT uq_piloto_licenca UNIQUE (num_licenca),
    CONSTRAINT fk_piloto_funcionario FOREIGN KEY (fk_Funcionario_matricula)
        REFERENCES Funcionario (matricula)
        ON DELETE CASCADE
);

-- =====================================================================
-- 8. COMISSARIO [especialização de Funcionario]
-- =====================================================================
CREATE TABLE Comissario (
    fk_Funcionario_matricula INT NOT NULL,
    CONSTRAINT pk_comissario PRIMARY KEY (fk_Funcionario_matricula),
    CONSTRAINT fk_comissario_funcionario FOREIGN KEY (fk_Funcionario_matricula)
        REFERENCES Funcionario (matricula)
        ON DELETE CASCADE
);

-- Atributo multivalorado: idioma(s) falado(s) pelo comissário
CREATE TABLE Comissario_Idioma (
    fk_Comissario_matricula INT         NOT NULL,
    idioma                  VARCHAR(30) NOT null default "portugues",
    CONSTRAINT pk_comissario_idioma PRIMARY KEY (fk_Comissario_matricula, idioma),
    CONSTRAINT fk_comissario_idioma_com FOREIGN KEY (fk_Comissario_matricula)
        REFERENCES Comissario (fk_Funcionario_matricula)
        ON DELETE CASCADE
);

-- =====================================================================
-- 9. MECANICO [especialização de Funcionario]
-- =====================================================================
CREATE TABLE Mecanico (
    fk_Funcionario_matricula INT NOT NULL,
    especialidade            VARCHAR(50) default "Geral",
    CONSTRAINT pk_mecanico PRIMARY KEY (fk_Funcionario_matricula),
    CONSTRAINT fk_mecanico_funcionario FOREIGN KEY (fk_Funcionario_matricula)
        REFERENCES Funcionario (matricula)
        ON DELETE CASCADE
);

-- =====================================================================
-- 10. PASSAGEIRO
-- =====================================================================
CREATE TABLE Passageiro (
    cpf           CHAR(11)    NOT NULL,
    primeiro_nome VARCHAR(50) NOT NULL,
    sobrenome     VARCHAR(50) NOT NULL,
    data_nasci    DATE,
    email         VARCHAR(100),
    CONSTRAINT pk_passageiro PRIMARY KEY (cpf)
);

-- Atributo multivalorado: telefone(s) do passageiro
CREATE TABLE Passageiro_Telefone (
    cpf      CHAR(11)    NOT NULL,
    telefone VARCHAR(20) NOT NULL,
    CONSTRAINT pk_passageiro_telefone PRIMARY KEY (cpf, telefone),
    CONSTRAINT fk_passageiro_telefone_pass FOREIGN KEY (cpf)
        REFERENCES Passageiro (cpf)
        ON DELETE CASCADE
);

-- =====================================================================
-- 11. VOO
-- =====================================================================
CREATE TABLE Voo (
    num_voo                     INT          NOT NULL,
    data_voo                    DATE         NOT NULL,
    hora_partida                TIME,
    hora_chegada                TIME,
    status                      VARCHAR(20),
    fk_Aeronave_cod_aeronave    VARCHAR(10)  NOT NULL,
    fk_Aeroporto_origem         VARCHAR(3)   NOT NULL,
    fk_Aeroporto_destino        VARCHAR(3)   NOT NULL,
    fk_Portao_num_portao        INT          NOT NULL,
    fk_Portao_num_terminal      INT          NOT NULL,
    fk_Voo_conexao              INT          NULL,
    fk_Piloto_comandante        INT          NOT NULL,
    CONSTRAINT pk_voo PRIMARY KEY (num_voo),
    CONSTRAINT fk_voo_aeronave FOREIGN KEY (fk_Aeronave_cod_aeronave)
        REFERENCES Aeronave (cod_aeronave),
    CONSTRAINT fk_voo_aeroporto_origem FOREIGN KEY (fk_Aeroporto_origem)
        REFERENCES Aeroporto (cod_iata),
    CONSTRAINT fk_voo_aeroporto_destino FOREIGN KEY (fk_Aeroporto_destino)
        REFERENCES Aeroporto (cod_iata),
    CONSTRAINT fk_voo_portao FOREIGN KEY (fk_Portao_num_portao, fk_Portao_num_terminal)
        REFERENCES Portao (num_portao, num_terminal),
    CONSTRAINT fk_voo_conexao FOREIGN KEY (fk_Voo_conexao)
        REFERENCES Voo (num_voo),
    CONSTRAINT fk_voo_piloto FOREIGN KEY (fk_Piloto_comandante)
        REFERENCES Piloto (fk_Funcionario_matricula),
    CONSTRAINT ck_voo_aeroportos_diferentes CHECK (fk_Aeroporto_origem <> fk_Aeroporto_destino)
);

-- =====================================================================
-- 12. ESCALA [relacionamento N:N entre Funcionario e Voo]
-- =====================================================================
CREATE TABLE Escala (
    fk_Funcionario_matricula INT NOT NULL,
    fk_Voo_num_voo           INT NOT NULL,
    funcao_no_voo            VARCHAR(30),
    CONSTRAINT pk_escala PRIMARY KEY (fk_Funcionario_matricula, fk_Voo_num_voo),
    CONSTRAINT fk_escala_funcionario FOREIGN KEY (fk_Funcionario_matricula)
        REFERENCES Funcionario (matricula)
        ON DELETE CASCADE,
    CONSTRAINT fk_escala_voo FOREIGN KEY (fk_Voo_num_voo)
        REFERENCES Voo (num_voo)
        ON DELETE CASCADE
);

-- =====================================================================
-- 13. MANUTENCAO [entidade fraca de Aeronave]
-- =====================================================================
CREATE TABLE Manutencao (
    cod_manutencao           INT         NOT NULL,
    fk_Aeronave_cod_aeronave VARCHAR(10) NOT NULL,
    data_inicio              DATE        NOT NULL,
    data_fim                 DATE,
    descricao                VARCHAR(500),
    fk_Mecanico_matricula    INT         NOT NULL,
    CONSTRAINT pk_manutencao PRIMARY KEY (cod_manutencao, fk_Aeronave_cod_aeronave),
    CONSTRAINT fk_manutencao_aeronave FOREIGN KEY (fk_Aeronave_cod_aeronave)
        REFERENCES Aeronave (cod_aeronave)
        ON DELETE CASCADE,
    CONSTRAINT fk_manutencao_mecanico FOREIGN KEY (fk_Mecanico_matricula)
        REFERENCES Mecanico (fk_Funcionario_matricula)
);

-- =====================================================================
-- 14. ASSENTO [entidade fraca de Aeronave]
-- =====================================================================
CREATE TABLE Assento (
    num_assento              VARCHAR(5)  NOT NULL,
    fk_Aeronave_cod_aeronave VARCHAR(10) NOT NULL,
    CONSTRAINT pk_assento PRIMARY KEY (num_assento, fk_Aeronave_cod_aeronave),
    CONSTRAINT fk_assento_aeronave FOREIGN KEY (fk_Aeronave_cod_aeronave)
        REFERENCES Aeronave (cod_aeronave)
        ON DELETE CASCADE
);

-- =====================================================================
-- 15. BILHETE [entidade associativa: Passageiro compra, referente a Voo/Assento]
-- =====================================================================
CREATE TABLE Bilhete (
    cod_bilhete              INT           NOT NULL,
    classe                   VARCHAR(20) default "economica",
    preco                    DECIMAL(10,2),
    data_compra              DATE,
    fk_Passageiro_cpf        CHAR(11)      NOT NULL,
    fk_Voo_num_voo           INT           NOT NULL,
    fk_Assento_num_assento   VARCHAR(5)    NOT NULL,
    fk_Assento_cod_aeronave  VARCHAR(10)   NOT NULL,
    CONSTRAINT pk_bilhete PRIMARY KEY (cod_bilhete),
    CONSTRAINT fk_bilhete_passageiro FOREIGN KEY (fk_Passageiro_cpf)
        REFERENCES Passageiro (cpf),
    CONSTRAINT fk_bilhete_voo FOREIGN KEY (fk_Voo_num_voo)
        REFERENCES Voo (num_voo),
    CONSTRAINT fk_bilhete_assento FOREIGN KEY (fk_Assento_num_assento, fk_Assento_cod_aeronave)
        REFERENCES Assento (num_assento, fk_Aeronave_cod_aeronave),
    CONSTRAINT uq_bilhete_voo_assento UNIQUE (fk_Voo_num_voo, fk_Assento_num_assento, fk_Assento_cod_aeronave)
);

-- =====================================================================
-- 16. BAGAGEM [entidade fraca de Bilhete]
-- =====================================================================
CREATE TABLE Bagagem (
    num_etiqueta           INT NOT NULL,
    fk_Bilhete_cod_bilhete INT NOT NULL,
    peso_kg                DECIMAL(5,2) check (peso_kg < 10),
    CONSTRAINT pk_bagagem PRIMARY KEY (num_etiqueta, fk_Bilhete_cod_bilhete),
    CONSTRAINT fk_bagagem_bilhete FOREIGN KEY (fk_Bilhete_cod_bilhete)
        REFERENCES Bilhete (cod_bilhete)
        ON DELETE CASCADE
);

-- =====================================================================
-- ÍNDICES auxiliares (melhoram as consultas do projeto)
-- =====================================================================
CREATE INDEX idx_voo_data ON Voo (data_voo);
CREATE INDEX idx_voo_aeronave ON Voo (fk_Aeronave_cod_aeronave);
CREATE INDEX idx_voo_portao ON Voo (fk_Portao_num_portao, fk_Portao_num_terminal);
CREATE INDEX idx_bilhete_voo ON Bilhete (fk_Voo_num_voo);
CREATE INDEX idx_escala_voo ON Escala (fk_Voo_num_voo);
CREATE INDEX idx_manutencao_aeronave ON Manutencao (fk_Aeronave_cod_aeronave);
CREATE INDEX idx_aeronave_cia ON Aeronave (fk_CompanhiaAerea_cod_cia);

