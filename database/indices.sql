-- =====================================================================
-- Aeroporto-BD — Módulo 2: Índices adicionais
-- Execute depois de migrations.sql e queries.sql.
-- =====================================================================
USE aeroporto;

-- Índice 1: Bilhete.fk_Passageiro_cpf
-- Por quê: a PK de Bilhete é cod_bilhete; não existe índice para buscar
-- "todos os bilhetes de um passageiro". Usado pela Consulta 11
-- (anti-join: passageiros sem bilhete) e pela view vw_passageiro_resumo
-- — sem índice, o LEFT JOIN Passageiro -> Bilhete varre Bilhete inteiro
-- para cada passageiro.
CREATE INDEX idx_bilhete_passageiro ON Bilhete (fk_Passageiro_cpf);

-- Índice 2: Voo.fk_Piloto_comandante
-- Por quê: usado pela Consulta 12 (full outer join Piloto x Voo) e já
-- seria útil também para o ranking de pilotos do módulo anterior
-- (RANK() OVER). Sem índice, o JOIN Piloto -> Voo varre Voo inteiro
-- para cada piloto.
CREATE INDEX idx_voo_piloto ON Voo (fk_Piloto_comandante);

-- Índice 3: Bagagem.fk_Bilhete_cod_bilhete
-- Por quê: a PK de Bagagem é composta (num_etiqueta, fk_Bilhete_cod_bilhete),
-- com fk_Bilhete_cod_bilhete na segunda posição — não serve para buscas
-- "todas as bagagens de um bilhete". Usado pela Consulta 14 (subconsulta
-- NOT EXISTS: bilhetes sem bagagem despachada).
CREATE INDEX idx_bagagem_bilhete ON Bagagem (fk_Bilhete_cod_bilhete);
